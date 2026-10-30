-- =====================================================================
--  Biblioteca · Datos de prueba
--  Las fechas son relativas al día actual para que siempre haya préstamos
--  activos, vencidos y devueltos al hacer la demostración.
--  Cervantes y Carlota Pérez-Reverte no tienen fecha de nacimiento: la de
--  Cervantes es incierta y, al ser anterior al calendario gregoriano (1582),
--  Java la mostraría desplazada 10 días.
-- =====================================================================

INSERT INTO autor (id, nombre, apellidos, nacionalidad, fecha_nacimiento) VALUES
    (1,  'Miguel de', 'Cervantes Saavedra', 'Española',   NULL),
    (2,  'Gabriel',   'García Márquez',     'Colombiana', DATE '1927-03-06'),
    (3,  'Isabel',    'Allende',            'Chilena',    DATE '1942-08-02'),
    (4,  'Carlos',    'Ruiz Zafón',         'Española',   DATE '1964-09-25'),
    (5,  'Terry',     'Pratchett',          'Británica',  DATE '1948-04-28'),
    (6,  'Neil',      'Gaiman',             'Británica',  DATE '1960-11-10'),
    (7,  'George',    'Orwell',             'Británica',  DATE '1903-06-25'),
    (8,  'Arturo',    'Pérez-Reverte',      'Española',   DATE '1951-11-25'),
    (9,  'Carlota',   'Pérez-Reverte',      'Española',   NULL),
    (10, 'J. R. R.',  'Tolkien',            'Británica',  DATE '1892-01-03'),
    (11, 'Almudena',  'Grandes',            'Española',   DATE '1960-05-07');

INSERT INTO libro (id, isbn, titulo, editorial, anio_publicacion, genero, ejemplares) VALUES
    (1,  '978-84-376-2405-1', 'Don Quijote de la Mancha',        'Cátedra',       1605, 'Novela',           3),
    (2,  '978-84-9759-245-1', 'Cien años de soledad',            'Debolsillo',    1967, 'Novela',           2),
    (3,  '978-84-01-24214-4', 'La casa de los espíritus',        'Plaza & Janés', 1982, 'Novela',           1),
    (4,  '978-84-08-16346-6', 'La sombra del viento',            'Planeta',       2001, 'Misterio',         2),
    (5,  '978-84-450-7180-9', 'Buenos presagios',                'Timun Mas',     1990, 'Fantasía',         1),
    (6,  '978-84-9989-361-7', '1984',                            'Debolsillo',    1949, 'Distopía',         2),
    (7,  '978-84-9989-362-4', 'Rebelión en la granja',           'Debolsillo',    1945, 'Fábula',           1),
    (8,  '978-84-204-8325-2', 'El capitán Alatriste',            'Alfaguara',     1996, 'Aventuras',        1),
    (9,  '978-84-450-0010-6', 'El hobbit',                       'Minotauro',     1937, 'Fantasía',         2),
    (10, '978-84-450-0901-7', 'El señor de los anillos',         'Minotauro',     1954, 'Fantasía',         1),
    (11, '978-84-8383-555-5', 'Inés y la alegría',               'Tusquets',      2010, 'Novela histórica', 1),
    (12, '978-84-9759-246-8', 'Crónica de una muerte anunciada', 'Debolsillo',    1981, 'Novela',           1),
    (13, '978-84-9838-124-5', 'American Gods',                   'Roca',          2001, 'Fantasía',         1),
    (14, '978-84-08-08236-1', 'El juego del ángel',              'Planeta',       2008, 'Misterio',         1);

-- N:M: "Buenos presagios" y "El capitán Alatriste" tienen dos autores
INSERT INTO libro_autor (libro_id, autor_id) VALUES
    (1, 1), (2, 2), (3, 3), (4, 4), (5, 5), (5, 6), (6, 7), (7, 7),
    (8, 8), (8, 9), (9, 10), (10, 10), (11, 11), (12, 2), (13, 6), (14, 4);

-- Cuentas de prueba: admin@example.com / Admin1234 (ADMIN) y los socios con Usuario1234 (USER)
INSERT INTO usuario (id, dni, nombre, apellidos, email, telefono, fecha_alta, password, rol) VALUES
    (1, '12345678Z', 'Laura',  'Martínez Soler',  'laura.martinez@example.com', '612345678', CURRENT_DATE - INTERVAL '400' DAY, '$2a$10$N9jbciwfU.zbT0LIjURgv.c5DF8hhlNjKZIL8aKNihDDqVD8pHWWC', 'USER'),
    (2, '23456781R', 'Javier', 'López Ferrer',    'javier.lopez@example.com',   '623456789', CURRENT_DATE - INTERVAL '300' DAY, '$2a$10$N9jbciwfU.zbT0LIjURgv.c5DF8hhlNjKZIL8aKNihDDqVD8pHWWC', 'USER'),
    (3, '34567812P', 'Marta',  'Sánchez Vidal',   'marta.sanchez@example.com',  '634567890', CURRENT_DATE - INTERVAL '200' DAY, '$2a$10$N9jbciwfU.zbT0LIjURgv.c5DF8hhlNjKZIL8aKNihDDqVD8pHWWC', 'USER'),
    (4, '45678123P', 'Pablo',  'Gómez Ruiz',      'pablo.gomez@example.com',    '645678901', CURRENT_DATE - INTERVAL '150' DAY, '$2a$10$N9jbciwfU.zbT0LIjURgv.c5DF8hhlNjKZIL8aKNihDDqVD8pHWWC', 'USER'),
    (5, '56781234F', 'Lucía',  'Fernández Torres','lucia.fernandez@example.com', NULL,       CURRENT_DATE - INTERVAL '90' DAY, '$2a$10$N9jbciwfU.zbT0LIjURgv.c5DF8hhlNjKZIL8aKNihDDqVD8pHWWC', 'USER'),
    (6, '67812345L', 'Daniel', 'Navarro Pons',    'daniel.navarro@example.com', '667890123', CURRENT_DATE - INTERVAL '30' DAY, '$2a$10$N9jbciwfU.zbT0LIjURgv.c5DF8hhlNjKZIL8aKNihDDqVD8pHWWC', 'USER'),
    (7, '00000000T', 'Admin',  'Biblioteca',      'admin@example.com',          NULL,        CURRENT_DATE - INTERVAL '400' DAY, '$2a$10$J2L4zCXTzv0DVowOMThXmuejzDSDMVI6r8QnkGA/l4W1Ft.tAiQeC', 'ADMIN');

INSERT INTO prestamo (id, usuario_id, libro_id, fecha_prestamo, fecha_devolucion_prevista, fecha_devolucion) VALUES
    -- Devueltos
    (1,  1, 1,  CURRENT_DATE - INTERVAL '120' DAY, CURRENT_DATE - INTERVAL '105' DAY, CURRENT_DATE - INTERVAL '110' DAY),
    (2,  2, 6,  CURRENT_DATE - INTERVAL '90' DAY,  CURRENT_DATE - INTERVAL '75' DAY,  CURRENT_DATE - INTERVAL '76' DAY),
    (3,  3, 5,  CURRENT_DATE - INTERVAL '60' DAY,  CURRENT_DATE - INTERVAL '45' DAY,  CURRENT_DATE - INTERVAL '44' DAY),
    (4,  1, 9,  CURRENT_DATE - INTERVAL '45' DAY,  CURRENT_DATE - INTERVAL '30' DAY,  CURRENT_DATE - INTERVAL '32' DAY),
    -- Vencidos (sin devolver y fuera de plazo)
    (5,  4, 3,  CURRENT_DATE - INTERVAL '25' DAY,  CURRENT_DATE - INTERVAL '10' DAY,  NULL),
    (6,  2, 10, CURRENT_DATE - INTERVAL '20' DAY,  CURRENT_DATE - INTERVAL '5' DAY,   NULL),
    -- Activos (sin devolver y dentro de plazo)
    (7,  1, 2,  CURRENT_DATE - INTERVAL '5' DAY,   CURRENT_DATE + INTERVAL '10' DAY,  NULL),
    (8,  3, 4,  CURRENT_DATE - INTERVAL '3' DAY,   CURRENT_DATE + INTERVAL '12' DAY,  NULL),
    (9,  5, 8,  CURRENT_DATE - INTERVAL '2' DAY,   CURRENT_DATE + INTERVAL '13' DAY,  NULL),
    (10, 6, 9,  CURRENT_DATE - INTERVAL '1' DAY,   CURRENT_DATE + INTERVAL '14' DAY,  NULL),
    (11, 2, 1,  CURRENT_DATE,                      CURRENT_DATE + INTERVAL '15' DAY,  NULL);
