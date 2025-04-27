-- ==========================================================
-- Drop and recreate the database.
-- ==========================================================
DROP DATABASE IF EXISTS moviedb;
CREATE DATABASE moviedb
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE       utf8mb4_0900_ai_ci;
USE moviedb;

-- ==========================================================
-- TABLE: movies
-- ==========================================================
CREATE TABLE movies (
                        id        VARCHAR(10)  NOT NULL,
                        title     VARCHAR(100) NOT NULL DEFAULT '',
                        year      INT          NOT NULL,
                        director  VARCHAR(100) NOT NULL DEFAULT '',
                        PRIMARY KEY (id)
) ENGINE=InnoDB;

-- ==========================================================
-- TABLE: stars
--  - Only 'birthYear' is optional.
-- ==========================================================
CREATE TABLE stars (
                       id         VARCHAR(10)  NOT NULL,
                       name       VARCHAR(100) NOT NULL DEFAULT '',
                       birthYear  INT               NULL,
                       PRIMARY KEY (id)
) ENGINE=InnoDB;

-- ==========================================================
-- TABLE: stars_in_movies
--  - Composite PK of (starId, movieId).
--  - References 'stars(id)' and 'movies(id)'.
-- ==========================================================
CREATE TABLE stars_in_movies (
                                 starId   VARCHAR(10) NOT NULL,
                                 movieId  VARCHAR(10) NOT NULL,
                                 PRIMARY KEY (starId, movieId),
                                 CONSTRAINT fk_sim_star
                                     FOREIGN KEY (starId)
                                         REFERENCES stars (id)
                                         ON UPDATE CASCADE
                                         ON DELETE RESTRICT,
                                 CONSTRAINT fk_sim_movie
                                     FOREIGN KEY (movieId)
                                         REFERENCES movies (id)
                                         ON UPDATE CASCADE
                                         ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ==========================================================
-- TABLE: genres
--  - 'id' auto-increments, 'name' required.
--  - A UNIQUE KEY on name is optional but can help prevent
--    duplicate genre names.
-- ==========================================================
CREATE TABLE genres (
                        id   INT           NOT NULL AUTO_INCREMENT,
                        name VARCHAR(32)   NOT NULL DEFAULT '',
                        PRIMARY KEY (id)
    -- Optional uniqueness if you prefer:
    -- , UNIQUE KEY (name)
) ENGINE=InnoDB;

-- ==========================================================
-- TABLE: genres_in_movies
--  - Composite PK (genreId, movieId).
--  - References 'genres.id' and 'movies.id'.
-- ==========================================================
CREATE TABLE genres_in_movies (
                                  genreId INT         NOT NULL,
                                  movieId VARCHAR(10) NOT NULL,
                                  PRIMARY KEY (genreId, movieId),
                                  CONSTRAINT fk_gim_genre
                                      FOREIGN KEY (genreId)
                                          REFERENCES genres (id)
                                          ON UPDATE CASCADE
                                          ON DELETE RESTRICT,
                                  CONSTRAINT fk_gim_movie
                                      FOREIGN KEY (movieId)
                                          REFERENCES movies (id)
                                          ON UPDATE CASCADE
                                          ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ==========================================================
-- TABLE: creditcards
--  - All attributes are required.
-- ==========================================================
CREATE TABLE creditcards (
                             id         VARCHAR(20)  NOT NULL,
                             firstName  VARCHAR(50)  NOT NULL DEFAULT '',
                             lastName   VARCHAR(50)  NOT NULL DEFAULT '',
                             expiration DATE         NOT NULL,
                             PRIMARY KEY (id)
) ENGINE=InnoDB;

-- ==========================================================
-- TABLE: customers
--  - 'id' auto-increments, everything else required
--    except 'ccId' references creditcards.id
--  - Optionally add UNIQUE KEY on email.
-- ==========================================================
CREATE TABLE customers (
                           id         INT          NOT NULL AUTO_INCREMENT,
                           firstName  VARCHAR(50)  NOT NULL DEFAULT '',
                           lastName   VARCHAR(50)  NOT NULL DEFAULT '',
                           ccId       VARCHAR(20)  NOT NULL,  -- referencing creditcards
                           address    VARCHAR(200) NOT NULL DEFAULT '',
                           email      VARCHAR(50)  NOT NULL DEFAULT '',
                           password   VARCHAR(20)  NOT NULL DEFAULT '',
                           PRIMARY KEY (id),
                           CONSTRAINT fk_customers_cc
                               FOREIGN KEY (ccId)
                                   REFERENCES creditcards (id)
                                   ON UPDATE CASCADE
                                   ON DELETE RESTRICT,
                           UNIQUE KEY (email)            -- optional; ensures unique emails
) ENGINE=InnoDB;

-- ==========================================================
-- TABLE: sales
--  - 'id' auto-increments, references 'customers.id' and 'movies.id'
-- ==========================================================
CREATE TABLE sales (
                       id          INT         NOT NULL AUTO_INCREMENT,
                       customerId  INT         NOT NULL,
                       movieId     VARCHAR(10) NOT NULL,
                       saleDate    DATE        NOT NULL,
                       PRIMARY KEY (id),
                       KEY ix_sales_customer (customerId),
                       CONSTRAINT fk_sales_customer
                           FOREIGN KEY (customerId)
                               REFERENCES customers (id)
                               ON UPDATE CASCADE
                               ON DELETE RESTRICT,
                       CONSTRAINT fk_sales_movie
                           FOREIGN KEY (movieId)
                               REFERENCES movies (id)
                               ON UPDATE CASCADE
                               ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ==========================================================
-- TABLE: ratings
--  - PK is movieId, references 'movies(id)'
-- ==========================================================
CREATE TABLE ratings (
                         movieId  VARCHAR(10) NOT NULL,
                         rating   FLOAT       NOT NULL,
                         numVotes INT         NOT NULL,
                         PRIMARY KEY (movieId),
                         CONSTRAINT fk_ratings_movie
                             FOREIGN KEY (movieId)
                                 REFERENCES movies (id)
                                 ON UPDATE CASCADE
                                 ON DELETE RESTRICT
) ENGINE=InnoDB;
