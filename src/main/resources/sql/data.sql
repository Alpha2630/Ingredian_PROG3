insert into dish (id, name, dish_type)
values (1, 'Salaide fraîche', 'STARTER'),
       (2, 'Poulet grillé', 'MAIN'),
       (3, 'Riz aux légumes', 'MAIN'),
       (4, 'Gâteau au chocolat ', 'DESSERT'),
       (5, 'Salade de fruits', 'DESSERT');


insert into ingredient (id, name, category, price, id_dish)
values (1, 'Laitue', 'VEGETABLE', 800.0, 1),
       (2, 'Tomate', 'VEGETABLE', 600.0, 1),
       (3, 'Poulet', 'ANIMAL', 4500.0, 2),
       (4, 'Chocolat ', 'OTHER', 3000.0, 4),
       (5, 'Beurre', 'DAIRY', 2500.0, 4);



update dish
set price = 2000.0
where id = 1;

update dish
set price = 6000.0
where id = 2;


update ingredient set required_quantity = 1 where name = 'Laitue';
update ingredient set required_quantity = 2 where name = 'Tomate';
update ingredient set required_quantity = 0.5 where name = 'Poulet';
update ingredient set required_quantity = null where name = 'Chocolat';
update ingredient set required_quantity = null where name = 'Beurre';

INSERT INTO dish (name, dish_type, price) VALUES
('Salade fraîche', 'STARTER', 2000),
('Poulet grillé', 'MAIN', 6000);

INSERT INTO ingredient (name, category, price) VALUES
('Laitue', 'VEGETABLE', 800),
('Tomate', 'VEGETABLE', 600),
('Poulet', 'ANIMAL', 4500),
('Huile', 'OTHER', 3000);

INSERT INTO dish_ingredient VALUES
(1, 1, 1, 'PIECE'),
(1, 2, 0.25, 'KG'),
(2, 3, 0.5, 'KG'),
(2, 4, 0.15, 'L');

