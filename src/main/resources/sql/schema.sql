create type dish_type as enum ('STARTER', 'MAIN', 'DESSERT', 'BEVERAGE');


create table dish
(
    id        serial primary key,
    name      varchar(255),
    dish_type dish_type,
    price     numeric(10,2)
);

create type ingredient_category as enum ('VEGETABLE', 'FRUIT', 'MEAT', 'FISH', 'DAIRY', 'SPICE', 'GRAIN', 'OTHER');

create table ingredient
(
    id       serial primary key,
    name     varchar(255),
    price    numeric(10, 2),
    category ingredient_category,
    id_dish  int references dish (id)
);
CREATE TYPE unit_type AS ENUM ('KG', 'G', 'L', 'ML', 'PIECE', 'CUP', 'TABLESPOON', 'TEASPOON');
CREATE TABLE dish_ingredient (
    dish_id INT REFERENCES dish(id) ON DELETE CASCADE,
    ingredient_id INT REFERENCES ingredient(id) ON DELETE CASCADE,
    required_quantity NUMERIC(10,2),
    unit unit_type,
    PRIMARY KEY (dish_id, ingredient_id)
);


alter table dish
    add column if not exists price numeric(10, 2);


alter table ingredient
    add column if not exists required_quantity numeric(10, 2);

