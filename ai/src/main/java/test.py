import h2o
from h2o.estimators import H2ORandomForestEstimator
import pymysql.cursors
import pandas as pd

# 初始化H2O集群
h2o.init()

# 从MySQL读取数据
connection = pymysql.connect(host='172.168.10.3',
                             user='root',
                             password='123456',
                             db='traceability',
                             charset='utf8mb4',
                             cursorclass=pymysql.cursors.DictCursor)

try:
    with connection.cursor() as cursor:
        # Replace with your SQL query
        sql = "SELECT	tenant_id,create_by,create_time,`no`,	input,usage_and_dosage,production_operation,operation_description,job_start_time,job_duration,job_introduce,random_job,job_by,`status`,	grow_plan_id FROM	t_production_schedule ORDER BY	create_time DESC "
        cursor.execute(sql)
        results = cursor.fetchall()
        
        # Convert results to pandas DataFrame
        df = pd.DataFrame(results)
        
        # Convert pandas DataFrame to H2OFrame
        hf = h2o.H2OFrame(df)
finally:
    connection.close()

print("H2OFrame的列：")
print(hf.columns)
hf['operation_description'] = hf['operation_description'].asfactor()
# 假设我们的目标列是"operation_description"，其余的是特征
features = [col for col in hf.columns if col != 'operation_description']
target = 'operation_description'

# 切分数据集
train, test = hf.split_frame(ratios=[0.8])

# 训练随机森林模型
model = H2ORandomForestEstimator()
model.train(x=features, y=target, training_frame=train)

# 评估模型
performance = model.model_performance(test_data=test)
print(performance)

# 保存模型到文件
# 保存模型到指定文件名
model_path = "/opt/machine/model/"
h2o.save_model(model=model, path=model_path, force=True)
print(model_path)
#pojo_path = model.download_pojo(path="/opt/machine/modelpo", get_genmodel_jar=True)
#print(pojo_path)

