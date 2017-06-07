/usr/local/mysql/bin/mysqladmin --host=localhost --user=$2 --password=$3 --force drop $1
/usr/local/mysql/bin/mysqladmin --host=localhost --user=$2 --password=$3 --force create $1
/usr/local/mysql/bin/mysql --host=localhost --user=$2 --password=$3 --database=$1 < /Users/David/Documents/workspace/WholesalerWebDemo/install/ebus.sql

