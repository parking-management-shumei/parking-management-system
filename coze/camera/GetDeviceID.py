import os
from runtime import Args
from typings.GetDeviceID.GetDeviceID import Input, Output
import mysql.connector

db=mysql.connector.connect(
    host=os.environ["DB_HOST"],
    port=int(os.environ.get("DB_PORT", "3306")),
    user=os.environ["DB_USER"],
    password=os.environ["DB_PASSWORD"],
    connect_timeout=30,
    database=os.environ["DB_NAME"]
)

def handler(args: Args[Input])->Output:
    input = args.input
    target =  input.target
    results = select_mysql(target)
    parking_id = solve(results)
    db.close
    return {"parking_id": parking_id}

def select_mysql(target:str)->list:
    cursor = db.cursor()
    sql = "select parking_id from parking_lots_data where parking_name = %s"
    cursor.execute(sql, (target,))
    results = cursor.fetchall()
    # db.close()
    return results

def solve(results:list)->str:
    parking_id = ""
    for result in results:
        parking_id = result
    parking_id = parking_id[0]
    return parking_id
