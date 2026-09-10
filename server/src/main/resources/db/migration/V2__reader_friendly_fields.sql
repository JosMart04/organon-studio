-- ===========================================================================
-- Campos para el lector aficionado
--
-- El cuaderno deja de poblarse solo con la semilla: ahora se crea a mano
-- mientras se lee, y hacen falta dos cosas que el modelo academico no tenia.
-- ===========================================================================

ALTER TABLE philosopher ADD COLUMN avatar_emoji VARCHAR(16);

ALTER TABLE passage ADD COLUMN personal_notes TEXT;

COMMENT ON COLUMN philosopher.avatar_emoji IS
    'Emoji con el que el lector identifica al pensador de un vistazo. Si esta vacio la interfaz pinta sus iniciales.';

COMMENT ON COLUMN passage.personal_notes IS
    'Lo que el lector piensa del pasaje, deliberadamente separado de text_content, que es lo que el pasaje dice.';
