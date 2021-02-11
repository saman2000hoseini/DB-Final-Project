create or replace FUNCTION user_register(
    uname varchar(50),
    pass varchar(50),
    fname varchar(100),
    lname varchar(100),
    addr varchar(500),
    utype varchar
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
        return 4;
    end if;

    perform regexp_matches(uname, '([A-Za-z0-9_]+)');
    if not FOUND then
        return 5;
    end if;

    insert into users(user_name, password, first_name, last_name, address, user_type)
    values (uname,
            sha512(pass::bytea)::varchar(512),
            fname,
            lname,
            addr,
            utype::u_type);

    return 0;
end
$$;

create type login_response as
(
    code  int,
    token varchar(512),
    utype u_type
);

-- drop function user_login;

create or replace function user_login(uname varchar(50), pass varchar(50))
    returns login_response
    language plpgsql
as
$$
declare
    temp  varchar(512);
    ts    timestamp;
    utype u_type;
    res   login_response;
begin
    select password, user_type
    into temp, utype
    from users
    where lower(uname) = lower(user_name);
    if not FOUND then
        select 1, null, null into res;
        return res;
    end if;

    if temp = sha512(pass::bytea)::varchar(512) then
        select CURRENT_TIMESTAMP + interval '1 hour' into ts;

        select 0, sha512(concat(uname, pass, ts)::bytea)::varchar(512), utype into res;
        update users
        set token      = res.token,
            expires_at = ts
        where lower(user_name) = lower(uname);
        return res;
    end if;

    select 2, null, null into res;
    return res;
end
$$;

create type account as
(
    uname      varchar,
    first_name varchar,
    last_name  varchar,
    address    varchar,
    user_type  u_type,
    credit     int,
    created_at timestamp
);

create or replace function user_get_info(utoken varchar(512))
    returns account
    language sql
as
$$
select user_name,
       first_name,
       last_name,
       address,
       user_type,
       credit,
       created_at as account
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

create type book_info as
(
    b_title     varchar,
    b_author    varchar,
    b_volume    int,
    b_edition   int,
    b_publisher varchar,
    b_published date,
    b_subject   varchar,
    b_pages     int,
    b_price     int
);

create or replace function search_book(
    title varchar,
    bauthor varchar,
    edition int,
    bpublisher varchar,
    published date
)
    returns setof book_info
    language sql
as
$$
select book.book_title,
       author,
       book.book_volume,
       book.book_edition,
       publisher.publisher_name,
       published_at,
       book_subject,
       book_pages,
       book_price
from book
         left outer join book_author on book.book_title = book_author.book_title and
                                        book.book_volume = book_author.book_volume and
                                        book.book_edition = book_author.book_edition
         left outer join publisher on book.publisher = publisher.publisher_id
where (title is null or title = '' or book.book_title = title)
  and (bauthor is null or bauthor = '' or author = bauthor)
  and (edition is null or book.book_edition = edition)
  and (published is null or published_at = published)
  and (bpublisher is null or bpublisher = '' or publisher_name = bpublisher)
order by book_title;
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
    return new;
end
$$;

create trigger update_on_borrow
    after insert
    on borrow
    for each row
execute procedure update_on_borrow();

create type user_info as
(
    first_name varchar(100),
    last_name  varchar(100),
    address    varchar(500),
    credit     int,
    user_type  u_type,
    created_at timestamp
);

create or replace function user_search(
    utoken varchar(512),
    u_name VARCHAR(50)
)
    returns user_info
    language plpgsql
as
$$
declare
    result user_info;
begin
    if utoken is null or u_name is null then
        return null;
    end if;

    perform user_name
    from users
    where token = utoken
      and expires_at >= current_timestamp
      and (user_type = 'admin'
        or user_type = 'librarian');
    if not found then
        return null;
    end if;

    select first_name, last_name, address, credit, user_type, created_at
    into result
    from users
    where lower(user_name) = lower(u_name);

    return result;
end
$$;

create or replace function user_remove(
    utoken varchar(512),
    u_name VARCHAR(50)
)
    returns int4
    language plpgsql
as
$$
begin
    if utoken is null or u_name is null then
        return 1;
    end if;

    perform user_name
    from users
    where token = utoken
      and expires_at >= current_timestamp
      and user_type = 'admin';
    if not found then
        return 2;
    end if;

    delete
    from users
    where lower(user_name) = lower(u_name);

    return 0;
end
$$;

create or replace function book_add(
    utoken varchar(512),
    amount int,
    b_title VARCHAR(250),
    b_edition int8 default 1,
    b_volume int8 default 0
)
    returns int4
    language plpgsql
as
$$
begin
    if utoken is null or amount is null or b_title is null then
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

    perform book_title
    from book
    where book_title = b_title
      and book_edition = b_edition
      and book_volume = b_volume;
    if not FOUND then
        return 3;
    end if;

    perform book_title
    from storage
    where book_title = b_title
      and book_edition = b_edition
      and book_volume = b_volume;
    if FOUND then
        update storage
        set book_amount = book_amount += amount
        where book_title = b_title
          and book_edition = b_edition
          and book_volume = b_volume;
        return 0;
    end if;

    insert into storage values (b_title, b_volume, b_edition, amount);
    return 0;
end
$$;

create or replace function book_borrow(
    utoken varchar(512),
    b_title VARCHAR(250),
    b_edition int8 default 1,
    b_volume int8 default 0
)
    returns int4
    language plpgsql
as
$$
declare
    amount  int;
    ucredit int;
    utype   u_type;
    suspend timestamp;
    btype   b_type;
    bprice  int;
    uname   varchar(50);
begin
    if utoken is null or b_title is null then
        return 1;
    end if;

    select user_name, suspended, credit, user_type
    into uname, suspend, ucredit, utype
    from users
    where token = utoken
      and expires_at >= current_timestamp;
    if not found then
        return 2;
    end if;

    if suspend >= CURRENT_TIMESTAMP then
        return 3;
    end if;

    select book_price, book_type, book_amount
    into bprice, btype, amount
    from book
             natural join storage
    where book_title = b_title
      and book_volume = b_volume
      and book_edition = b_edition;
    if not found then
        return 4;
    end if;

    if amount < 1 then
        return 5;
    end if;

    if utype = 'normal' then
        if btype = 'educational' or btype = 'reference' then
            return 6;
        end if;
    end if;

    if utype = 'student' then
        if btype = 'reference' then
            return 6;
        end if;
    end if;

    if ucredit < (bprice * 5 / 100) then
        return 7;
    end if;

    insert into borrow(user_name, book_title, book_edition, book_volume, cost)
    values (uname, b_title, b_edition, b_volume, (bprice * 5 / 100));
    return 0;
end
$$;

create or replace function book_return(
    utoken varchar(512),
    b_title VARCHAR(250),
    b_edition int8 default 1,
    b_volume int8 default 0
)
    returns int4
    language plpgsql
as
$$
declare
    uname varchar(50);
begin
    if utoken is null or b_title is null then
        return 1;
    end if;

    select user_name
    into uname
    from users
    where token = utoken
      and expires_at >= current_timestamp;
    if not found then
        return 2;
    end if;

    perform *
    from borrow
    where user_name = uname
      and book_title = b_title
      and book_volume = b_volume
      and book_edition = b_edition
      and borrow_return_date is null;
    if not FOUND then
        return 3;
    end if;

    update storage
    set book_amount = book_amount + 1
    where book_title = b_title
      and book_volume = b_volume
      and book_edition = b_edition;

    update borrow
    set borrow_return_date = CURRENT_DATE
    where user_name = uname
      and book_title = b_title
      and book_volume = b_volume
      and book_edition = b_edition
      and borrow_return_date is null;

    return 0;
end
$$;


create or replace function update_on_return()
    returns trigger
    language plpgsql
as
$$
declare
    sdate       date := CURRENT_DATE - interval '2 month';
    delay_count int;
begin
    select count(*)
    into delay_count
    from borrow
    where user_name = new.user_name
      and borrow_return_date > sdate
      and borrow_return_date > borrow.borrow_date + (borrow_duration || ' day')::interval;
    if delay_count > 3 then
        update users
        set suspended = CURRENT_DATE + interval '1 month'
        where user_name = new.user_name;
    end if;

    update storage
    set book_amount = book_amount + 1
    where book_title = new.book_title
      and book_volume = new.book_volume
      and book_edition = new.book_edition;
    return new;
end
$$;

create trigger update_on_return
    after update
    on borrow
    for each row
execute procedure update_on_return();

create type borrow_info as
(
    user_name          varchar,
    first_name         varchar,
    last_name          varchar,
    user_type          u_type,
    borrow_date        date,
    borrow_return_date date
);

create or replace function borrow_search(
    utoken varchar(512),
    b_title VARCHAR(250),
    b_edition int8 default 1,
    b_volume int8 default 0
)
    returns setof borrow_info
    language plpgsql
as
$$
declare
    result borrow_info;
begin
    if utoken is null or b_title is null then
        return;
    end if;

    perform user_name
    from users
    where token = utoken
      and expires_at >= current_timestamp
      and (user_type = 'admin'
        or user_type = 'librarian');
    if not found then
        return;
    end if;

    for result in select user_name, first_name, last_name, user_type, borrow_date, borrow_return_date
                  from borrow
                           natural join users
                  where book_title = b_title
                    and book_volume = b_volume
                    and book_edition = b_edition
                  order by borrow_date desc
        loop
            return next result;
        end loop;
    return;
end
$$;

create or replace function user_add_credit(
    utoken varchar(512),
    amount int
)
    returns int4
    language plpgsql
as
$$
begin
    if utoken is null or amount is null then
        return 1;
    end if;

    perform user_name
    from users
    where token = utoken
      and expires_at >= current_timestamp;
    if not found then
        return 2;
    end if;

    if amount < 0 then
        return 3;
    end if;

    update users
    set credit = credit + amount
    where token = utoken;

    return 0;
end
$$;