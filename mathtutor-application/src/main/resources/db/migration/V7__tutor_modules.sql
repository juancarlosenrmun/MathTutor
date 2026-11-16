-- UC-12: bloques fijados por RF-02; todos los alumnos pueden seleccionarlos.
INSERT INTO modules (code, name) VALUES
    ('funciones', 'Funciones'),
    ('derivadas-integrales', 'Derivadas e integrales'),
    ('metodos-3d', 'Métodos 3D')
ON CONFLICT (code) DO NOTHING;
