package studio.organon.server.search;

import studio.organon.server.search.dto.SearchHitType;

/** Una entrada del cuaderno tal como se indexa: el titulo y el texto que la representan ante el modelo. */
record IndexableItem(SearchHitType type, long id, String title, String text) {

    String key() {
        return key(type, id);
    }

    /** Clave compartida entre la base de datos, la memoria y la fusion de rankings: «PASSAGE:12». */
    static String key(SearchHitType type, long id) {
        return type.name() + ":" + id;
    }

    static SearchHitType typeOf(String key) {
        return SearchHitType.valueOf(key.substring(0, key.indexOf(':')));
    }

    static long idOf(String key) {
        return Long.parseLong(key.substring(key.indexOf(':') + 1));
    }
}
