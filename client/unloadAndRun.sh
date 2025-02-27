#!/bin/bash

#script para facilitar el unzip y la ejecucion de los clientes
pwd=`pwd`

clientes=("administrationClient.sh" "waitingRoomClient.sh" "emergencyCareClient.sh" "doctorPagerClient.sh" "queryClient.sh")

cd target || exit
tar -xzf tpe1-g7-client-1.0-SNAPSHOT-bin.tar.gz
for i in "${!clientes[@]}"; do
  echo "$i: ${clientes[$i]}"
done
sleep 1
read -p "presione numero de cliente: " opcion


if [ -n "$opcion" ] && [ "$opcion" -le 4 ] && [ "$opcion" -ge 0 ]; then
  cd tpe1-g7-client-1.0-SNAPSHOT || exit

  sh "${clientes[opcion]}" "$*"
  cd "$pwd" || exit
  else
    echo "invalid option specified..."
fi


