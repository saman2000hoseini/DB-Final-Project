create database library;
--
-- drop table *;
-- drop type u_type;
create type u_type as ENUM ('normal', 'student', 'instructor', 'librarian', 'admin');

create table users
(
    user_name  varchar(50) primary key check ( length(user_name) > 5 ),
    password   varchar(512) not null,
    first_name varchar(100) not null,
    last_name  varchar(100) not null,
    address    varchar(500) not null,
    credit     int                   default 0,
    user_type  u_type       not null,
    created_at timestamp    not null default CURRENT_TIMESTAMP,
    suspended  timestamp    null,
    token      varchar(512) null,
    expires_at timestamp    null
);

CREATE TABLE publisher
(
    publisher_id      bigint generated always as identity primary key,
    publisher_name    VARCHAR(80),
    publisher_address VARCHAR(250) NOT NULL,
    publisher_website VARCHAR(250)
);

create type b_type as ENUM ('normal', 'educational', 'reference');

CREATE TABLE book
(
    book_title   VARCHAR(250),
    book_volume  int8 DEFAULT 0,
    book_edition int8 DEFAULT 1,
    book_subject VARCHAR(250),
    book_price   INT    NOT NULL,
    book_pages   INT    NOT NULL,
    book_type    b_type not null,
    published_at date   not null,
    publisher    bigint,
    PRIMARY KEY (book_title, book_volume, book_edition),
    Foreign Key (publisher) references publisher (publisher_id)
);

CREATE TABLE book_author
(
    book_title   varchar(250),
    book_volume  int8 DEFAULT 0,
    book_edition int8 DEFAULT 1,
    author       VARCHAR(80),
    PRIMARY KEY (book_title, book_volume, book_edition, author),
    FOREIGN KEY (book_title, book_volume, book_edition)
        REFERENCES Book (book_title, book_volume, book_edition)
        ON DELETE CASCADE
);

CREATE TABLE storage
(
    book_title   varchar(250),
    book_volume  int8 DEFAULT 0,
    book_edition int8 DEFAULT 1,
    book_amount  INT,
    PRIMARY KEY (book_title, book_volume, book_edition),
    FOREIGN KEY (book_title, book_volume, book_edition)
        REFERENCES Book (book_title, book_volume, book_edition)
);

CREATE TABLE borrow
(
    user_name          varchar(50),
    book_title         varchar(250)                   NOT NULL,
    book_volume        int8 DEFAULT 0,
    book_edition       int8 DEFAULT 1,
    borrow_date        date default CURRENT_TIMESTAMP not null,
    borrow_duration    int8 default 14,
    borrow_return_date date                           null,
    cost               int                            not null,
    PRIMARY KEY (user_name, book_title, book_volume, book_edition, borrow_date),
    FOREIGN KEY (book_title, book_volume, book_edition)
        REFERENCES Book (book_title, book_volume, book_edition),
    foreign key (user_name) references users (user_name)
        ON DELETE CASCADE
);

-- drop table book_author, book;
-- drop table storage, borrow;

create type u_operation as ENUM ('borrow', 'return');

create type o_result as ENUM ('success', 'returned with delay', 'book not available', 'low credit', 'book does not exists');

create table operation_history
(
    id               bigint generated always as identity primary key,
    user_name        varchar(50) not null,
    user_operation   u_operation not null,
    operation_result o_result    not null,
    operation_time   timestamp default current_timestamp,
    foreign key (user_name) references users (user_name)
        ON DELETE CASCADE
)