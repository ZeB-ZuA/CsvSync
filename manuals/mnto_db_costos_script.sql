-- Script para migrar el calculo de repuestos de Excel a SQL



--main table
CREATE TABLE costos2025 (
    num_ot char(25),
	bus char(50),
	fecha_origen timestamp,
	status_ot char(3),
	fecha_cierre timestamp,
	area char(15),
	especialidad char(15),
	tipo_reporte char(15),
	trab_requerido TEXT,
	cod_falla char(15),
	cod_accion char(15),
	cod_causa char(15),
	kms char(20),
	ot_trab_realizado TEXT,
	cod_actividad char(25),
	act_tiporeporte char(15),
	act_trab_requerido TEXT,
	num_rm float,
	codigo_item_srv char(25),
	desc_item_srv TEXT,
	cant_usada float,
	udm char(8),
	costo_unitario float,
	subtotal float,
	fecha_uso timestamp,
	num_rt float,
	rt_area char(15),
	rt_especialidad char(15),
	rt_trab_requerido TEXT,
	rt_trab_realizado TEXT,
	planeador_mtlrs char(30),
	ubicacion char(30),
	ref_id BIGINT PRIMARY KEY,
	especialidad_rec char(50),
    area_rec char(50) NULL,
    ubicacion2 char(50) NULL,
    costo NUMERIC NULL,
    mes char(50) NULL,
    anio INT NULL,
    tipo char(50) NULL,
    frecuencia char(50) NULL,
    componente char(50) NULL,
    tipologia char(50) NULL,
    tipo_manto char(50) NULL
);

-- Indexes for costos2025
CREATE INDEX IF NOT EXISTS idx_costos2025_cod_actividad ON costos2025 (cod_actividad);
CREATE INDEX IF NOT EXISTS idx_costos2025_especialidad_rec ON costos2025 (especialidad_rec);
CREATE INDEX IF NOT EXISTS idx_costos2025_ref_id ON costos2025 (ref_id);
CREATE INDEX IF NOT EXISTS idx_costos2025_ubicacion ON costos2025 (ubicacion);

-- Trigger  to update 'area_rec' after insert
CREATE TRIGGER update_area_rec_trigger
    AFTER INSERT ON costos2025
    FOR EACH ROW
    EXECUTE FUNCTION update_area_rec_after_insert();
-- Trigger to update 'ubicacion2' before insert
CREATE TRIGGER update_ubicacion2_and_tipo_trigger
    AFTER INSERT ON costos2025
    FOR EACH ROW
    EXECUTE FUNCTION update_ubicacion2_and_tipo_before_insert();
-- Trigger to update 'costo' before insert
CREATE TRIGGER update_costo_trigger
    BEFORE INSERT ON costos2025
    FOR EACH ROW
    EXECUTE FUNCTION update_costo_before_insert();
-- Trigger to update 'mes' and 'anio' before insert
CREATE TRIGGER update_mes_anio_trigger
    BEFORE INSERT ON costos2025
    FOR EACH ROW
    EXECUTE FUNCTION update_mes_anio_before_insert();
-- Trigger to update 'frecuencia' before insert
CREATE TRIGGER update_frecuencia_trigger
    BEFORE INSERT ON costos2025
    FOR EACH ROW
    EXECUTE FUNCTION update_frecuencia_before_insert();
-- Trigger to update 'tipologia' before insert
CREATE TRIGGER update_tipologia_trigger
    BEFORE INSERT ON costos2025
    FOR EACH ROW
    EXECUTE FUNCTION update_tipologia_before_insert();
-- Trigger to update 'tipo_manto' before insert
CREATE TRIGGER update_tipo_manto_trigger
    BEFORE INSERT ON costos2025
    FOR EACH ROW
    EXECUTE FUNCTION update_tipo_manto_before_insert();


    --Main event trigger on Create Table.
    CREATE OR REPLACE FUNCTION add_area_rec_to_presupuesto_tables()
    RETURNS EVENT_TRIGGER AS $$
    DECLARE
        tbl_name TEXT;
    BEGIN
        -- Recupera el nombre de la tabla creada
        SELECT objid::regclass::text INTO tbl_name
        FROM pg_event_trigger_ddl_commands()
        WHERE command_tag = 'CREATE TABLE';
        
        -- Log: Imprime el nombre de la tabla que se está creando
        RAISE NOTICE 'Tabla creada: %', tbl_name;
        
        -- Comprueba si el nombre de la tabla empieza con 'pres_%'
        IF tbl_name LIKE 'pres_%' THEN
            BEGIN
                -- Agrega la columna 'area_rec' si no existe
                EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS area_rec VARTEXT;', tbl_name);
                EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS ubicacion2 VARTEXT;', tbl_name);
                EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS costo NUMERIC;', tbl_name);
                EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS mes VARCHAR(50);', tbl_name);
                EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS anio INT;', tbl_name);
                EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS tipo VARCHAR(50)', tbl_name);
                EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS frecuencia VARCHAR(50)', tbl_name);
                EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS componente VARTEXT', tbl_name);
                EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS tipologia VARCHAR(50)', tbl_name);
                EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS tipo_manto VARCHAR(50)', tbl_name);
            EXCEPTION WHEN OTHERS THEN
                RAISE WARNING 'Error al añadir la columna "area_rec" en la tabla %: %', tbl_name, SQLERRM;
            END;

            BEGIN
                --Add primary key to ref_id column
                EXECUTE format('ALTER TABLE %I ADD PRIMARY KEY (ref_id);', tbl_name);
                -- Agrega los índices para las columnas específicas
                EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%I_cod_actividad ON %I (cod_actividad);', tbl_name, tbl_name);
                EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%I_especialidad_rec ON %I (especialidad_rec);', tbl_name, tbl_name);
                EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%I_ref_id ON %I (ref_id);', tbl_name, tbl_name);
                EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%I_ubicacion ON %I (ubicacion);', tbl_name, tbl_name);

                RAISE NOTICE 'Índices creados para la tabla: %', tbl_name;
            EXCEPTION WHEN OTHERS THEN
                RAISE WARNING 'Error al crear los índices en la tabla %: %', tbl_name, SQLERRM;
            END;

            BEGIN
                -- Agrega la columna 'id' como serial si no existe (si esta función está definida)
               -- PERFORM add_serial_column_to_existing_tables();
            EXCEPTION WHEN OTHERS THEN
                RAISE WARNING 'Error al agregar la columna serial en la tabla %: %', tbl_name, SQLERRM;
            END;

            -- Crear el trigger AFTER INSERT para la tabla recién creada
            BEGIN
                RAISE NOTICE 'Creando el trigger AFTER INSERT para la tabla: %', tbl_name;

                EXECUTE format('
                    CREATE TRIGGER update_area_rec_trigger
                    AFTER INSERT ON %I
                    FOR EACH ROW
                    EXECUTE FUNCTION update_area_rec_after_insert();',
                    tbl_name);
                EXECUTE format('
                    CREATE TRIGGER update_ubicacion2_trigger
                    BEFORE INSERT ON %I
                    FOR EACH ROW
                    EXECUTE FUNCTION update_ubicacion2_and_tipo_before_insert();',
                    tbl_name);
                EXECUTE format('
                    CREATE TRIGGER update_costo_trigger
                    BEFORE INSERT ON %I
                    FOR EACH ROW
                    EXECUTE FUNCTION update_costo_before_insert();',
                    tbl_name);
                EXECUTE format('
                    CREATE TRIGGER update_mes_anio_trigger
                    BEFORE INSERT ON %I
                    FOR EACH ROW
                    EXECUTE FUNCTION update_mes_anio_before_insert();',
                    tbl_name);
                EXECUTE format('
                    CREATE TRIGGER update_frecuencia_trigger
                    BEFORE INSERT ON %I
                    FOR EACH ROW
                    EXECUTE FUNCTION update_frecuencia_before_insert();',
                    tbl_name);
                EXECUTE format('
                    CREATE TRIGGER update_tipologia_trigger
                    BEFORE INSERT ON %I
                    FOR EACH ROW
                    EXECUTE FUNCTION update_tipologia_before_insert();',
                    tbl_name);
                EXECUTE format('
                    CREATE TRIGGER update_tipo_manto_trigger
                    BEFORE INSERT ON %I
                    FOR EACH ROW
                    EXECUTE FUNCTION update_tipo_manto_before_insert();',
                    tbl_name);
            EXCEPTION WHEN OTHERS THEN
                RAISE WARNING 'Error al crear el trigger AFTER INSERT en la tabla %: %', tbl_name, SQLERRM;
            END;
        END IF;
    END;
    $$ LANGUAGE plpgsql;

-- Evento para ejecutar la función cuando se cree una nueva tabla
CREATE EVENT TRIGGER add_area_rec_to_presupuesto
ON ddl_command_end
WHEN TAG IN ('CREATE TABLE')
EXECUTE FUNCTION add_area_rec_to_presupuesto_tables();



CREATE OR REPLACE FUNCTION update_tipo_manto_before_insert()
RETURNS TRIGGER AS $$
BEGIN
    NEW.tipo_manto:=COALESCE(
        (SELECT tipo_manto FROM reporte_manto WHERE tipo_reporte = NEW.act_tiporeporte LIMIT 1),
        'Valor no encontrado'
    );
RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION update_tipologia_before_insert()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.bus LIKE 'Z91-4%' THEN
        NEW.tipologia := 'BUSETON';
    ELSE
        NEW.tipologia := 'PADRON';
    END IF;
    RETURN NEW;
END
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION update_frecuencia_before_insert()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.num_ot LIKE 'P-%' THEN
        NEW.frecuencia := SUBSTRING(NEW.num_ot FROM 12 FOR 4);
    ELSE
       
        NEW.frecuencia := '';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;



CREATE OR REPLACE FUNCTION update_ubicacion2_and_tipo_before_insert()
RETURNS TRIGGER AS $$
BEGIN
    -- Calcular ubicacion2
    IF NEW.ubicacion IS NOT NULL AND NEW.ubicacion <> '' THEN
        NEW.ubicacion2 := NEW.ubicacion;
    ELSE
        NEW.ubicacion2 := COALESCE(
            (SELECT tipo_servicio
             FROM servicios
             WHERE servicio = NEW.cod_actividad
             LIMIT 1),
            'Valor no encontrado'  -- Valor por defecto si no se encuentra tipo_servicio
        );
    END IF;

    -- Calcular tipo en base a ubicacion2
    IF NEW.ubicacion2 LIKE '%SERVICIO%' THEN
        NEW.tipo := 'SERVICIO';
    ELSE
        NEW.tipo := 'REPUESTO';
    END IF;

    RETURN NEW; -- Devuelve la fila con los valores actualizados
END;
$$ LANGUAGE plpgsql;


$$ LANGUAGE plpgsql;




CREATE OR REPLACE FUNCTION update_mes_anio_before_insert()
RETURNS TRIGGER AS $$
BEGIN
  NEW.mes := to_char(NEW.fecha_uso, 'Month');
  NEW.anio := EXTRACT(YEAR FROM NEW.fecha_uso);
  
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;





CREATE OR REPLACE FUNCTION update_costo_before_insert()
RETURNS TRIGGER AS $$
BEGIN
    -- Caso 1: Si 'ubicacion' está vacío o es NULL
    IF NEW.ubicacion IS NULL OR NEW.ubicacion = '' THEN
        NEW.costo := ROUND(
            COALESCE(
                (SELECT factor
                 FROM servicios
                 WHERE servicio = NEW.cod_actividad
                 LIMIT 1),
                1 -- Valor predeterminado si no encuentra un factor
            ) * NEW.subtotal::numeric, 2
        );

    -- Caso 2: Si 'ubicacion' tiene un valor
    ELSE
        NEW.costo := ROUND(1 * NEW.subtotal::numeric, 2);
    END IF;

    RETURN NEW; -- Devuelve la fila con el cálculo correcto
END;
$$ LANGUAGE plpgsql;










CREATE OR REPLACE FUNCTION update_area_rec_after_insert()
RETURNS TRIGGER AS $$
DECLARE
    target_table TEXT = TG_TABLE_NAME;
    update_query TEXT;
BEGIN
    -- Construye la consulta UPDATE de manera dinámica
    update_query := format('
        UPDATE %I
        SET area_rec = COALESCE(
                (SELECT area 
                 FROM especialidades_area 
                 WHERE especialidad = $1.especialidad_rec 
                 LIMIT 1),
                COALESCE(
                    (SELECT area 
                     FROM actividades_area 
                     WHERE actividad = $1.cod_actividad 
                     LIMIT 1),
                    (SELECT area 
                     FROM actividad1_area1 
                     WHERE LEFT($1.cod_actividad, 5) = actividad1_area1.actividad 
                     LIMIT 1)
                )
            )
        WHERE especialidad_rec = $1.especialidad_rec 
          AND cod_actividad = $1.cod_actividad;', target_table);

    -- Ejecuta la consulta UPDATE
    EXECUTE update_query USING NEW;

    RETURN NEW;  -- Regresa la fila recién insertada
END;
$$ LANGUAGE plpgsql;



-- Tablas de constantes
-- Tabla especialidades_area
CREATE TABLE
    especialidades_area (especialidad VARTEXT, area VARTEXT);
-- Indexes for especialidades_area
CREATE INDEX idx_especialidades_area_especialidad ON especialidades_area (especialidad);

INSERT INTO
    especialidades_area (especialidad, area)
VALUES
    ('ALINEACION', 'LLANTAS'),
    ('ALTO_VOLTAJE', 'ELECTRICIDAD'),
    ('BAJO_VOLTAJE', 'ELECTRICIDAD'),
    ('BATERIAS', 'ELECTRICIDAD'),
    ('CHAPA_LATONERIA', 'CARROCERIA'),
    ('COMUNICACIONES', 'ELECTRICIDAD'),
    ('DIRECCION', 'MECANICA'),
    ('FRENOS', 'MECANICA'),
    ('HABITACULO', 'CARROCERIA'),
    ('LAVADO', 'IMAGEN'),
    ('LLANTAS', 'LLANTAS'),
    ('LUBRICACION', 'LUBRICACION'),
    ('MOTRIZ', 'MECANICA'),
    ('NEUMATICO', 'MECANICA'),
    ('PINTURA', 'IMAGEN'),
    ('PUERTAS', 'CARROCERIA'),
    ('REFRIGERACION', 'MECANICA'),
    ('RODAJE', 'MECANICA'),
    ('SUSPENSION', 'MECANICA'),
    ('VIDRIOS', 'CARROCERIA'),
    ('ATT', 'INSTRUMENTOS'),
    ('INSTRUMENTOS', 'INSTRUMENTOS');


-- Tabla actividades_area
CREATE TABLE
    actividades_area (actividad VARTEXT, area VARTEXT);

-- Indexes for actividades_area
CREATE INDEX idx_actividades_area_actividad ON actividades_area (actividad); 

INSERT INTO
    actividades_area (actividad, area)
VALUES
    ('MEC01', 'MECANICA'),
    ('MEC02', 'MECANICA'),
    ('MEC03', 'MECANICA'),
    ('MEC04', 'MECANICA'),
    ('MEC05', 'MECANICA'),
    ('MEC06', 'MECANICA'),
    ('MEC07', 'MECANICA'),
    ('MEC08', 'MECANICA'),
    ('MEC09', 'MECANICA'),
    ('MEC10', 'MECANICA'),
    ('MEC11', 'MECANICA'),
    ('MEC12', 'MECANICA'),
    ('MEC13', 'MECANICA'),
    ('MEC14', 'MECANICA'),
    ('MEC15', 'MECANICA'),
    ('MEC16', 'MECANICA'),
    ('MEC17', 'MECANICA'),
    ('MEC18', 'MECANICA'),
    ('MEC19', 'MECANICA'),
    ('MEC20', 'MECANICA'),
    ('MEC100', 'MECANICA'),
    ('ELE01', 'ELECTRICIDAD'),
    ('ELE02', 'ELECTRICIDAD'),
    ('ELE03', 'ELECTRICIDAD'),
    ('ELE04', 'ELECTRICIDAD'),
    ('ELE05', 'ELECTRICIDAD'),
    ('ELE06', 'ELECTRICIDAD'),
    ('ELE07', 'ELECTRICIDAD'),
    ('ELE08', 'ELECTRICIDAD'),
    ('ELE09', 'ELECTRICIDAD'),
    ('ELE10', 'ELECTRICIDAD'),
    ('ELE11', 'ELECTRICIDAD'),
    ('ELE12', 'ELECTRICIDAD'),
    ('ELE13', 'ELECTRICIDAD'),
    ('ELE14', 'ELECTRICIDAD'),
    ('ELE15', 'ELECTRICIDAD'),
    ('ELE16', 'ELECTRICIDAD'),
    ('ELE17', 'ELECTRICIDAD'),
    ('ELE18', 'ELECTRICIDAD'),
    ('ELE19', 'ELECTRICIDAD'),
    ('ELE20', 'ELECTRICIDAD'),
    ('LLA01', 'LLANTAS'),
    ('LLA02', 'LLANTAS'),
    ('LLA03', 'LLANTAS'),
    ('LLA04', 'LLANTAS'),
    ('LLA05', 'LLANTAS'),
    ('LLA06', 'LLANTAS'),
    ('LLA07', 'LLANTAS'),
    ('LLA08', 'LLANTAS'),
    ('LLA09', 'LLANTAS'),
    ('LLA10', 'LLANTAS'),
    ('LLA11', 'LLANTAS'),
    ('LLA12', 'LLANTAS'),
    ('LLA13', 'LLANTAS'),
    ('LLA14', 'LLANTAS'),
    ('LLA15', 'LLANTAS'),
    ('LLA16', 'LLANTAS'),
    ('LLA17', 'LLANTAS'),
    ('LLA18', 'LLANTAS'),
    ('LLA19', 'LLANTAS'),
    ('LLA20', 'LLANTAS'),
    ('CAR01', 'CARROCERIA'),
    ('CAR02', 'CARROCERIA'),
    ('CAR03', 'CARROCERIA'),
    ('CAR04', 'CARROCERIA'),
    ('CAR05', 'CARROCERIA'),
    ('CAR06', 'CARROCERIA'),
    ('CAR07', 'CARROCERIA'),
    ('CAR08', 'CARROCERIA'),
    ('CAR09', 'CARROCERIA'),
    ('CAR10', 'CARROCERIA'),
    ('CAR11', 'CARROCERIA'),
    ('CAR12', 'CARROCERIA'),
    ('CAR13', 'CARROCERIA'),
    ('CAR14', 'CARROCERIA'),
    ('CAR15', 'CARROCERIA'),
    ('CAR16', 'CARROCERIA'),
    ('CAR17', 'CARROCERIA'),
    ('CAR18', 'CARROCERIA'),
    ('CAR19', 'CARROCERIA'),
    ('LUB01', 'LUBRICACION'),
    ('LUB02', 'LUBRICACION'),
    ('LUB03', 'LUBRICACION'),
    ('LUB04', 'LUBRICACION'),
    ('LUB05', 'LUBRICACION'),
    ('LUB06', 'LUBRICACION'),
    ('LUB07', 'LUBRICACION'),
    ('LUB08', 'LUBRICACION'),
    ('LUB09', 'LUBRICACION'),
    ('LUB10', 'LUBRICACION'),
    ('LUB11', 'LUBRICACION'),
    ('LUB12', 'LUBRICACION'),
    ('LUB13', 'LUBRICACION'),
    ('LUB14', 'LUBRICACION'),
    ('LUB15', 'LUBRICACION'),
    ('IMA01', 'IMAGEN'),
    ('IMA02', 'IMAGEN'),
    ('IMA03', 'IMAGEN'),
    ('IMA04', 'IMAGEN'),
    ('IMA05', 'IMAGEN'),
    ('IMA06', 'IMAGEN'),
    ('IMA07', 'IMAGEN'),
    ('IMA08', 'IMAGEN'),
    ('IMA09', 'IMAGEN'),
    ('IMA10', 'IMAGEN'),
    ('IMA11', 'IMAGEN'),
    ('IMA12', 'IMAGEN'),
    ('IMA13', 'IMAGEN'),
    ('IMA14', 'IMAGEN'),
    ('IMA15', 'IMAGEN'),
    ('IMA16', 'IMAGEN'),
    ('IMA17', 'IMAGEN'),
    ('IMA18', 'IMAGEN'),
    ('IMA19', 'IMAGEN'),
    ('IMA20', 'IMAGEN');

-- Tabla actividad1_area1
CREATE TABLE
    actividad1_area1 (actividad VARTEXT, area VARTEXT);
-- Indexes for actividad1_area1
CREATE INDEX idx_actividad1_area1_actividad ON actividad1_area1 (actividad);

INSERT INTO
    actividad1_area1 (actividad, area)
VALUES
    ('P-CAR', 'CARROCERIA'),
    ('P-LUB', 'LUBRICACION'),
    ('P-MEC', 'MECANICA'),
    ('P-ELE', 'ELECTRICIDAD'),
    ('P-IMA', 'IMAGEN'),
    ('P-LLA', 'LLANTAS');

-- tabla servicios
CREATE TABLE servicios (
    servicio VARTEXT,
    subtotal TEXT,
    factor NUMERIC ,
    tipo_servicio VARTEXT
);
-- Crear índices después de crear la tabla
CREATE INDEX idx_servicio ON servicios(servicio);
CREATE INDEX idx_factor ON servicios(factor);
CREATE INDEX idx_tipo_servicio ON servicios(tipo_servicio);

INSERT INTO servicios (servicio, subtotal, factor, tipo_servicio) VALUES
('SRVCHACORLAMINAESTRIBO', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVFABLAM', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVREPPUE', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVREPTORNIQUETE', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVINSPANTALLA', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVDESEQUIPOS', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVDOBCHA', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVINSMICRO', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVLLAREPMAYPADRON', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVLLAREPMAYBUSETON', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVLLAREPMAY', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVLLAREPMEN', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVPULVID', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SERRECROSCOM', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SERRECROSTIM', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SERCAMACRIDISTOR', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVREVPRE', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SERRECROSCOMP', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SERRECDISFRE', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SERSUMTINPEN', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVREPSUS', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVFABCOM', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVREPTAPSIL', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVDESEQUIPOS', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVREPTAPSIL', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SERCAMTOUSCRUL', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SERRECTECROS', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SERRECROSTOP', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVSUMLAMPF06', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVREPESCPS1', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SERCAMANTGPRS', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVSUMINSBRATOR', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVSUMDISACRITOR', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVSUMLAMPF05', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVSUMLAMPF06MAR', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVSUMLAMPF07', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVSUMLAMPM01', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVREPCALFRE', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('REPSOPBMBALVT', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SERRECTECROSMOT', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SERRECTECROSMOT', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SERREPESTFLDPM02', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SERRECTECROSBASEST', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SERRECROSELE', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SERREPPALNIVGUAYA', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SERREPVALAIRINT', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SERSUMEMPVALRES', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SERSUMINSNIVAMOR', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SERREPCORSILOPE', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SERSUMCJNTVAROLT', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVSUMVALALTGNRKB', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SERRECROSMECEJE', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SERMANLUBBAL', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVSUMCAMDISTOR', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SERACTFMWBVDR', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVREPROSAMOR', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVREPVIGSUSP', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SERFABGUA', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVREPCONS', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('CAR01', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVREPDEFBYD', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVREPVID', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SERMANENFCOM', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVREPTAPSILLOP', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVRECROSSEN', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVRECUROSDIS', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVALIXC', '(SUBTOTAL IGUAL)', 1, 'SERVICIO CORRECTIVO'),
('SRVLAVINTE', 'Qty * Costo unitario *1,019', 1.019, 'SERVICIO COSTO FIJO'),
('SRVLAVINTEBUSETON', 'Qty * Costo unitario *1,019', 1.019, 'SERVICIO COSTO FIJO'),
('SRVLLANMES', 'Qty * Costo unitario *1,19', 1.19, 'SERVICIO COSTO FIJO'),
('SRVREPPIN', '(SUBTOTAL IGUAL)', 1, 'SERVICIO COSTO FIJO'),
('SRVCABLAGFRT', '(SUBTOTAL IGUAL)', 1, 'SERVICIO COSTO FIJO'),
('SRVCABBMBALVT', '(SUBTOTAL IGUAL)', 1, 'SERVICIO COSTO FIJO');

CREATE TABLE reporte_manto (
    tipo_reporte VARCHAR(50),
    tipo_manto VARCHAR(50)
);
-- Indexes for reporte_manto
CREATE INDEX idx_tipo_reporte ON reporte_manto(tipo_reporte);
INSERT INTO reporte_manto (tipo_reporte, tipo_manto) VALUES
('ANUAL_TM', 'CORRECTIVO'),
('CAMPANA', 'MP'),
('COLISION', 'CORRECTIVO'),
('FALLA_RUTA', 'CORRECTIVO'),
('GARANTIA', 'CORRECTIVO'),
('MP', 'MP'),
('MTOXCONDICION', 'MP'),
('VANDALISMO', 'CORRECTIVO'),
('VARADO', 'CORRECTIVO'),
('GARANTIA_NOATEN', 'CORRECTIVO'),
('GARANTIA_NO ATEN', 'CORRECTIVO'),
('TELEMETRIA', 'CORRECTIVO'),
('CORRECTIVO', 'CORRECTIVO');


\COPY (SELECT ref_id, bus, area_rec, ubicacion2, costo, mes, anio, tipo, frecuencia, componente, tipologia, tipo_manto FROM presupuesto ORDER BY ref_id) TO 'C:/Users/TI/Desktop/presupuesto.csv' DELIMITER ';' CSV HEADER;





------------------------------Unused Functions----------------------------------------------



-- Función para actualizar el campo 'tipo' en la tabla 'presupuesto'
CREATE OR REPLACE FUNCTION update_tipo_before_insert()
RETURNS TRIGGER AS $$
BEGIN
    -- Si 'ubicacion2' contiene 'SERVICIO', asigna 'SERVICIO' a 'tipo'
    IF NEW.ubicacion2 LIKE '%SERVICIO%' THEN
        NEW.tipo := 'SERVICIO';
    ELSE
        NEW.tipo := 'REPUESTO';
    END IF;

    RETURN NEW; -- Devuelve la fila con el valor actualizado
END;




-- Función para agregar columna 'id' a todas las tablas
CREATE OR REPLACE FUNCTION add_serial_column_to_existing_tables()
RETURNS void AS
$$
DECLARE
    table_record RECORD;
    column_exists BOOLEAN;
BEGIN
    -- Recorre todas las tablas en el esquema público
    FOR table_record IN
        SELECT table_name
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_type = 'BASE TABLE'
    LOOP
        -- Verifica si la tabla ya tiene la columna 'id'
        SELECT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = table_record.table_name
              AND column_name = 'id'
        ) INTO column_exists;

        -- Si la columna 'id' no existe, agrégala
        IF NOT column_exists THEN
            EXECUTE 'ALTER TABLE public.' || table_record.table_name || ' ADD COLUMN id SERIAL PRIMARY KEY';
        END IF;
    END LOOP;
END;
$$
LANGUAGE plpgsql;



-- Consulta parta actualizar el campo 'area_rec' en la tabla 'presupuesto'
UPDATE presupuesto
SET area_rec = COALESCE(
    (SELECT area FROM especialidades_area WHERE especialidad = presupuesto.especialidad LIMIT 1),
    COALESCE(
        (SELECT area FROM actividades_area WHERE cod_actividad = presupuesto.cod_actividad LIMIT 1),
        (SELECT area
         FROM actividad1_area1
         WHERE LEFT(presupuesto.cod_actividad, 5) = actividad1_area1.actividad
         LIMIT 1)
    )
);

-- Función para actualizar el campo 'area_rec' en la tabla 'presupuesto'
CREATE OR REPLACE FUNCTION update_presupuesto_area_rec()
RETURNS VOID AS $$
BEGIN
    UPDATE presupuesto
    SET area_rec = COALESCE(
        (SELECT area FROM especialidades_area WHERE especialidad = presupuesto.especialidad LIMIT 1),
        COALESCE(
            (SELECT area FROM actividades_area WHERE cod_actividad = presupuesto.cod_actividad LIMIT 1),
            (SELECT area
             FROM actividad1_area1
             WHERE LEFT(presupuesto.cod_actividad, 5) = actividad1_area1.actividad
             LIMIT 1)
        )
    );
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION update_ubicacion2_and_tipo_before_insert_fuc()
RETURNS VOID AS $$
BEGIN
    -- Actualiza ubicacion2 con el valor de ubicacion si no está vacío
    UPDATE costos2025
    SET ubicacion2 = ubicacion
    WHERE ubicacion IS NOT NULL AND ubicacion <> '';

    -- Actualiza ubicacion2 con el valor de tipo_servicio de la tabla servicios si ubicacion está vacío
    UPDATE costos2025
    SET ubicacion2 = COALESCE(
        (SELECT tipo_servicio
         FROM servicios
         WHERE servicio = costos2025.cod_actividad
         LIMIT 1),
        'Valor no encontrado'  -- Valor por defecto si no se encuentra tipo_servicio
    )
    WHERE ubicacion IS NULL OR ubicacion = '';

    -- Actualiza tipo con 'SERVICIO' o 'REPUESTO' según el valor de ubicacion2
    UPDATE costos2025
    SET tipo = CASE 
                 WHEN ubicacion2 LIKE '%SERVICIO%' THEN 'SERVICIO'
                 ELSE 'REPUESTO'
               END
    WHERE ubicacion2 IS NOT NULL;
END;
$$ LANGUAGE plpgsql;




-- Función para actualizar el campo 'ubicacion' en la tabla 'presupuesto'
CREATE OR REPLACE FUNCTION update_ubicacion2()
RETURNS VOID AS $$
BEGIN
    -- Actualiza ubicacion2 con el valor de ubicacion si no está vacío
    UPDATE presupuesto
    SET ubicacion2 = ubicacion
    WHERE ubicacion IS NOT NULL AND ubicacion <> '';

    -- Actualiza ubicacion2 con el valor de tipo_servicio de la tabla servicios si ubicacion está vacío
    UPDATE presupuesto
    SET ubicacion2 = COALESCE(
        (SELECT tipo_servicio
         FROM servicios
         WHERE servicio = presupuesto.cod_actividad
         LIMIT 1),
        'Valor no encontrado'  -- Valor por defecto si no se encuentra tipo_servicio
    )
    WHERE ubicacion IS NULL OR ubicacion = '';
END;
$$ LANGUAGE plpgsql;

-- Función para actualizar el campo 'ubicacion' en la tabla 'presupuesto'

CREATE OR REPLACE FUNCTION update_ubicacion2_before_insert()
RETURNS TRIGGER AS $$
BEGIN
    -- Si ubicacion no está vacío, asigna su valor a ubicacion2
    IF NEW.ubicacion IS NOT NULL AND NEW.ubicacion <> '' THEN
        NEW.ubicacion2 := NEW.ubicacion;
    ELSE
        -- Si ubicacion está vacío, busca tipo_servicio en la tabla servicios
        NEW.ubicacion2 := COALESCE(
            (SELECT tipo_servicio
             FROM servicios
             WHERE servicio = NEW.cod_actividad
             LIMIT 1),
            'Valor no encontrado'  -- Valor por defecto si no se encuentra tipo_servicio
        );
    END IF;

    RETURN NEW; -- Devuelve la fila con los valores actualizados
END;
$$ LANGUAGE plpgsql;


--PowerShell script to process the SQL file

$filePath = "C:\Users\TI\Desktop\presupuestovscosto.sql"

$fileContent = Get-Content $filePath

$fileContent = $fileContent -replace '(\d+),(\d+)', '$1.$2'

$fileContent = $fileContent -replace '\btimestamp\b', 'timestamp'

$fileContent = $fileContent -replace 'char\(255\)', 'TEXT'

Set-Content -Path $filePath -Value $fileContent

