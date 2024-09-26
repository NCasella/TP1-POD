# TP1-POD
Para preparar el entorno del servidor y del cliente, primero se debe correr el siguiente comando en el root del repositorio

``mvn install ``

Luego, los pasos a seguir son similares tanto para el servidor como para el cliente.
## Servidor



Al cambiar el working directory al directorio *server*, se debe descomprimir el archivo .jar que se encuentra en el directorio server/target con:

``tar -xzf target/tpe1-g7-server-1.0-SNAPSHOT-bin.tar.gz -C target/``

Luego se debera entrar a la carpeta *tpe1-g7-server-1.0-SNAPSHOT* generada anteriormente, en el que se encontrara el script **run-server.sh**, que se puede ejecutar para lograr tener la instancia del servidor corriendo con:

``sh run-server.sh``

## Cliente


Los pasos a seguir son similares, con la excepcion de que se debe encontrar en el directorio *client* y correr:

``tar -xzf target/tpe1-g7-client-1.0-SNAPSHOT-bin.tar.gz -C target/``

Luego al entrar a la carpeta *tpe1-g7-client-1.0-SNAPSHOT* se encontraran los scripts para correr los respectivos clientes.

Para facilitar la descomprecion y la ejecucion de los clientes, se encuentra un script **unloadAndRun.sh**, que replica los pasos detallados anteriormente para el modulo del cliente:

``sh unloadAndRun.sh -Dargs...``

Al ejecutarse, el script le pedira al usuario por entrada estandar un numero que representa el cliente a correr.