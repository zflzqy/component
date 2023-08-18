import h2o
from h2o.estimators import H2ORandomForestEstimator
from h2o.grid.grid_search import H2OGridSearch
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
        sql = "SELECT tenant_id,create_by,DATE_FORMAT(create_time, '%%Y-%%m-%%d %%H:%%i:%%s') as create_time,`no`,input,usage_and_dosage,production_operation,operation_description, DATE_FORMAT(job_start_time, '%%Y-%%m-%%d') as job_start_time,job_duration,job_introduce,random_job,job_by,`status`,grow_plan_id FROM t_production_schedule ORDER BY create_time DESC"
        cursor.execute(sql)
        results = cursor.fetchall()
        
        df = pd.DataFrame(results)
        hf = h2o.H2OFrame(df)
finally:
    connection.close()

# 特征工程
hf['weekday'] = hf['create_time'].dayOfWeek()
hf['hour'] = hf['create_time'].hour()

hf['operation_description'] = hf['operation_description'].asfactor()

features = [col for col in hf.columns if col != 'operation_description']
target = 'operation_description'

train, test = hf.split_frame(ratios=[0.8])

# 调整随机森林的参数
rf_params = {
    'ntrees': [100, 150, 200],  # 使用更多的树
    'max_depth': [30, 40, 50] # 更深的树
}
rf_grid = H2OGridSearch(model=H2ORandomForestEstimator(nfolds=5),
                        grid_id='rf_grid',
                        hyper_params=rf_params)

rf_grid.train(x=features, y=target, training_frame=train)

# 获取准确度最高的模型
best_rf = rf_grid.get_grid(sort_by='accuracy', decreasing=True)[0]

performance = best_rf.model_performance(test_data=test)
print(performance)

model_path = "/opt/machine/model/"
h2o.save_model(model=best_rf, path=model_path, force=True)
print(model_path)

