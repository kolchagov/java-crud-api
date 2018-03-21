alter session set nls_date_format = 'YYYY-MM-DD HH24:MI:SS'
/

CREATE TABLE categories (
  id number(11) NOT NULL,
  name varchar(255) NOT NULL,
  icon blob NULL,
  PRIMARY KEY (id)
)
/


CREATE SEQUENCE categories_seq START WITH 1
/

CREATE OR REPLACE TRIGGER categories_autoinc
  BEFORE INSERT ON categories
  FOR EACH ROW
  BEGIN
    SELECT categories_seq.NEXTVAL
    INTO   :new.id
    FROM   dual;
  END;
/

INSERT INTO categories (id, name, icon) VALUES (1,  'announcement', NULL)
/
INSERT INTO categories (id, name, icon) VALUES (2,  'article',  NULL)
/


CREATE TABLE users (
  id number(11) NOT NULL,
  username varchar(255) NOT NULL,
  password varchar(255) NOT NULL,
  location SDO_GEOMETRY NULL,
  PRIMARY KEY (id)
)
/

CREATE SEQUENCE users_seq START WITH 1
/

CREATE OR REPLACE TRIGGER users_autoinc
  BEFORE INSERT ON users
  FOR EACH ROW
  BEGIN
    SELECT users_seq.NEXTVAL
    INTO   :new.id
    FROM   dual;
  END;
/

INSERT INTO users (id, username, password, location) VALUES (1, 'user1',  'pass1', null)
/
INSERT INTO users (id, username, password, location) VALUES (2, 'user2',  'pass2', null)
/


CREATE TABLE posts (
  id number(11) NOT NULL,
  user_id number(11) NOT NULL,
  category_id number(11) NOT NULL,
  content varchar(255) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT posts_ibfk_3 FOREIGN KEY (category_id) REFERENCES categories (id),
  CONSTRAINT posts_ibfk_4 FOREIGN KEY (user_id) REFERENCES users (id)
)
/

CREATE SEQUENCE posts_seq START WITH 1
/

CREATE OR REPLACE TRIGGER posts_autoinc
  BEFORE INSERT ON posts
  FOR EACH ROW
  BEGIN
    SELECT posts_seq.NEXTVAL
    INTO   :new.id
    FROM   dual;
  END;
/

INSERT INTO posts (id, user_id, category_id, content) VALUES (1,  1,  1,  'blog started')
/
INSERT INTO posts (id, user_id, category_id, content) VALUES (2,  1,  2,  'It works!')
/

CREATE TABLE comments (
  id number(11) NOT NULL,
  post_id number(11) NOT NULL,
  message varchar(255) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT comments_ibfk_1 FOREIGN KEY (post_id) REFERENCES posts (id)
)
/

CREATE SEQUENCE comments_seq START WITH 1
/

CREATE OR REPLACE TRIGGER comments_autoinc
  BEFORE INSERT ON comments
  FOR EACH ROW
  BEGIN
    SELECT comments_seq.NEXTVAL
    INTO   :new.id
    FROM   dual;
  END;
/

INSERT INTO comments (id, post_id, message) VALUES (1,  1,  'great')
/
INSERT INTO comments (id, post_id, message) VALUES (2,  1,  'fantastic')
/
INSERT INTO comments (id, post_id, message) VALUES (3,  2,  'thank you')
/
INSERT INTO comments (id, post_id, message) VALUES (4,  2,  'awesome')
/


CREATE TABLE tags (
  id number(11) NOT NULL,
  name varchar(255) NOT NULL,
  PRIMARY KEY (id)
)
/

CREATE SEQUENCE tags_seq START WITH 1
/

CREATE OR REPLACE TRIGGER tags_autoinc
  BEFORE INSERT ON tags
  FOR EACH ROW
  BEGIN
    SELECT tags_seq.NEXTVAL
    INTO   :new.id
    FROM   dual;
  END;
/

INSERT INTO tags (id, name) VALUES (1,  'funny')
/
INSERT INTO tags (id, name) VALUES (2,  'important')
/


CREATE TABLE post_tags (
  id number(11) NOT NULL,
  post_id number(11) NOT NULL,
  tag_id number(11) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT post_tags_ibfk_1 FOREIGN KEY (post_id) REFERENCES posts (id),
  CONSTRAINT post_tags_ibfk_2 FOREIGN KEY (tag_id) REFERENCES tags (id)
)
/

CREATE SEQUENCE post_tags_seq START WITH 1
/

CREATE OR REPLACE TRIGGER post_tags_autoinc
  BEFORE INSERT ON post_tags
  FOR EACH ROW
  BEGIN
    SELECT post_tags_seq.NEXTVAL
    INTO   :new.id
    FROM   dual;
  END;
/

INSERT INTO post_tags (id, post_id, tag_id) VALUES (1,  1,  1)
/
INSERT INTO post_tags (id, post_id, tag_id) VALUES (2,  1,  2)
/
INSERT INTO post_tags (id, post_id, tag_id) VALUES (3,  2,  1)
/
INSERT INTO post_tags (id, post_id, tag_id) VALUES (4,  2,  2)
/

CREATE TABLE countries (
  id number(11) NOT NULL,
  name varchar(255) NOT NULL,
  shape sdo_geometry NOT NULL,
  PRIMARY KEY (id)
)
/

CREATE SEQUENCE countries_seq START WITH 1
/

CREATE OR REPLACE TRIGGER countries_autoinc
  BEFORE INSERT ON countries
  FOR EACH ROW
  BEGIN
    SELECT countries_seq.NEXTVAL
    INTO   :new.id
    FROM   dual;
  END;
/

create or replace function ST_GeomFromText(p_shape VARCHAR) return sdo_geometry
is
  BEGIN
    return SDO_GEOMETRY(2003, NULL, NULL, SDO_ELEM_INFO_ARRAY(1, 1003, 1), SDO_ORDINATE_ARRAY(5, 1, 8, 1, 8, 6, 5, 7, 5, 1));
  END;
/

INSERT INTO countries (id, name, shape) VALUES (1,  'Left', ST_GeomFromText('POLYGON ((30 10, 40 40, 20 40, 10 20, 30 10))'))
/
INSERT INTO countries (id, name, shape) VALUES (2,  'Right',  ST_GeomFromText('POLYGON ((70 10, 80 40, 60 40, 50 20, 70 10))'))
/


CREATE TABLE events (
  id number(11) NOT NULL,
  name varchar(255) NOT NULL,
  datetime date NOT NULL,
  visitors number(11) NOT NULL,
  PRIMARY KEY (id)
)
/

CREATE SEQUENCE events_seq START WITH 1
/

CREATE OR REPLACE TRIGGER events_autoinc
  BEFORE INSERT ON events
  FOR EACH ROW
  BEGIN
    SELECT events_seq.NEXTVAL
    INTO   :new.id
    FROM   dual;
  END;
/


INSERT INTO events (id, name, datetime, visitors) VALUES (1, 'Launch', '2016-01-01 13:01:01', 0)
/

CREATE VIEW tag_usage AS select name, count(name) AS count from tags, post_tags where tags.id = post_tags.tag_id group by name order by count desc, name
/



CREATE TABLE products (
  id number(11) NOT NULL,
  name varchar(255) NOT NULL,
  price decimal(10,2) NOT NULL,
  properties clob NOT NULL,
  created_at date NOT NULL,
  deleted_at date NULL,
  PRIMARY KEY (id)
)
/

CREATE SEQUENCE products_seq START WITH 1
/

CREATE OR REPLACE TRIGGER products_autoinc
  BEFORE INSERT ON products
  FOR EACH ROW
  BEGIN
    SELECT products_seq.NEXTVAL
    INTO   :new.id
    FROM   dual;
  END;
/

INSERT INTO products (id, name, price, properties, created_at) VALUES
  (1, 'Calculator', '23.01', '{"depth":false,"model":"TRX-120","width":100,"height":null}', '1970-01-01 01:01:01')
/


CREATE TABLE barcodes (
  id number(11) NOT NULL,
  product_id number(11) NOT NULL,
  hex varchar(255) NOT NULL,
  bin blob NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT barcodes_ibfk_1 FOREIGN KEY (product_id) REFERENCES products (id)
)
/

CREATE SEQUENCE barcodes_seq START WITH 1
/

CREATE OR REPLACE TRIGGER barcodes_autoinc
  BEFORE INSERT ON barcodes
  FOR EACH ROW
  BEGIN
    SELECT barcodes_seq.NEXTVAL
    INTO   :new.id
    FROM   dual;
  END;
/

INSERT INTO barcodes (id, product_id, hex, bin) VALUES (1, 1, '00ff01', RAWTOHEX('00ff01'))
/