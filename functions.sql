create or replace FUNCTION user_register(
    uname varchar(50),
    pass varchar(50),
    fname varchar(100),
    lname varchar(100),
    addr varchar(500),
    utype u_type
)
    returns int
    language plpgsql
as
$$
begin
    if uname is null or pass is null or fname is null or lname is null or addr is null or utype is null then
        return 1;
    end if;

    if length(uname) < 6 then
        return 2;
    end if;

    if length(pass) < 8 then
        return 3;
    end if;

    perform user_name
    from users
    where lower(user_name) = lower(uname);
    if FOUND then
        return 3;
    end if;

    perform regexp_matches(uname, '([A-Za-z0-9_]+)');
    if not FOUND then
        return 4;
    end if;

    insert into users(user_name, password, first_name, last_name, address, user_type)
    values (uname,
            sha512(pass::bytea)::varchar(512),
            fname,
            lname,
            addr,
            utype);

    return 0;
end
$$;

create type login_response as
(
    code  int,
    token varchar(512)
);

-- drop function user_login;

create or replace function user_login(uname varchar(50), pass varchar(50))
    returns login_response
    language plpgsql
as
$$
declare
    temp varchar(512);
    ts   timestamp;
    res  login_response;
begin
    select password
    into temp
    from users
    where lower(uname) = lower(user_name);
    if not FOUND then
        select 1, null into res;
        return res;
    end if;

    if temp = sha512(pass::bytea)::varchar(512) then
        select CURRENT_TIMESTAMP + interval '1 hour' into ts;

        select 0, sha512(concat(uname, pass, ts)::bytea)::varchar(512) into res;
        update users
        set token      = res.token,
            expires_at = ts
        where lower(uname) = lower(uname);
        return res;
    end if;

    select 2, null into res;
    return res;
end
$$;

create or replace function user_get_info(utoken varchar(512))
    returns record
    language sql
as
$$
select user_name, first_name, last_name, address, user_type, credit, created_at
from users
where token = utoken
  and token is not null
  and expires_at >= CURRENT_TIMESTAMP;
$$;

create or replace function book_insert(
    utoken varchar(512),
    b_title VARCHAR(250),
    b_subject VARCHAR(250),
    b_price INT,
    b_pages INT,
    b_type b_type,
    published date,
    owner bigint,
    b_edition int8 default 1,
    b_volume int8 default 0
)
    returns int4
    language plpgsql
as
$$
begin
    if utoken is null or b_title is null or b_subject is null or b_price is null or b_pages is null or b_type is null or
       published is null or owner is null then
        return 1;
    end if;

    perform user_name
    from users
    where token = utoken
      and expires_at >= current_timestamp
      and (user_type = 'admin'
        or user_type = 'librarian');
    if not found then
        return 2;
    end if;

    perform book_title, book_volume, book_edition
    from book
    where book_title = b_title
      and book_volume = b_volume
      and book_edition = b_edition;
    if found then
        return 3;
    end if;

    insert into book(book_volume, book_edition, book_title, book_subject, book_price, book_pages, book_type,
                     published_at, publisher)
    values (b_volume, b_edition, b_title, b_subject, b_price, b_pages, b_type, published, owner);

    return 0;
end
$$;

create or replace function publisher_insert(
    utoken varchar(512),
    p_name VARCHAR(80),
    p_address VARCHAR(250),
    p_website VARCHAR(250)
)
    returns int4
    language plpgsql
as
$$
begin
    if utoken is null or p_name is null or p_address is null or p_website is null then
        return 1;
    end if;

    perform user_name
    from users
    where token = utoken
      and expires_at >= current_timestamp
      and (user_type = 'admin'
        or user_type = 'librarian');
    if not found then
        return 2;
    end if;

    perform publisher_name
    from publisher
    where publisher_name = p_name;
    if found then
        return 3;
    end if;

    insert into publisher(publisher_name, publisher_address, publisher_website)
    values (p_name, p_address, p_website);

    return 0;
end
$$;

create or replace function search_book(tile varchar, bauthor varchar, edition int, bpublisher varchar, published date)
    returns setof record
    language sql
as
$$
select *
from book
         natural join book_author
         natural join publisher
where (tile is null or book_title = tile)
  and (bauthor is null or author = bauthor)
  and (edition is null or book_edition = edition)
  and (published is null or published_at = published)
  and (bpublisher is null or publisher_name = bpublisher)
order by book_title
$$;

create or replace function update_on_borrow()
    returns trigger
    language plpgsql
as
$$
begin

    update users
    set credit = credit - new.cost
    where user_name = new.user_name;
    update storage
    set book_amount = book_amount - 1
    where book_title = new.book_title
      and book_volume = new.book_volume
      and book_edition = new.book_edition;
end
$$;

create trigger update_on_borrow
    after insert
    on borrow
    for each row
execute procedure update_on_borrow();
