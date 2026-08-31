Questa cartella e' prevista dalla struttura di consegna richiesta dal
docente (slide "Il Progetto - Consegna").

Nel caso di questo progetto non e' pero' necessaria: l'unica libreria
esterna usata (il driver JDBC di PostgreSQL) e' dichiarata come
dipendenza nel file pom.xml e viene scaricata automaticamente da
Maven durante la compilazione (mvn clean package), quindi non serve
includerne una copia manuale qui dentro.
