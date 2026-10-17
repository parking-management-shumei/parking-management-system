import os
from runtime import Args
from typings.Cloud_based_data.Cloud_based_data import Input, Output
import mysql.connector
import datetime

db=mysql.connector.connect(
    host=os.environ["DB_HOST"],
    port=int(os.environ.get("DB_PORT", "3306")),
    user=os.environ["DB_USER"],
    password=os.environ["DB_PASSWORD"],
    connect_timeout=30,
    database=os.environ["DB_NAME"]
)

def handler(args: Args[Input]):
    input = args.input
    logger = args.logger
    cursor = db.cursor()
    for data in input.data:
        device_id = data.device_id
        total = int(data.total)  # 将字符串转换为整数
        parking_id = device_id.split('-')[0]+'-'+device_id.split('-')[1]  # 提取parking_id


        # 检查device_id是否已存在
        sql = "SELECT camera_id FROM car_iden_data WHERE camera_id = %s "
        cursor.execute(sql,(device_id,))
        existing_camera = cursor.fetchone()

        if existing_camera:
             # 如果存在，更新数据
            update_sql = "UPDATE car_iden_data SET data = %s, last_time = %s WHERE camera_id = %s"
            cursor.execute(update_sql, (total, datetime.datetime.now(), device_id))
        else:
            # 如果不存在，插入新数据
            insert_sql = """
            INSERT INTO car_iden_data (camera_id, parking_id, data, last_time)
            VALUES (%s, %s, %s, %s)
            """
            cursor.execute(insert_sql, (device_id, parking_id, total, datetime.datetime.now()))

            # 提交事务
        db.commit()
    db.close
