from flask import Flask, request, jsonify
import h2o

app = Flask(__name__)

# 初始化H2O并加载模型
h2o.init()
model = h2o.load_model('/opt/machine/model/DRF_model_python_1692331434565_4')  # 修改为您的模型路径

@app.route('/predict', methods=['POST'])
def predict():
    data = request.json  # 获取JSON数据
    hf = h2o.H2OFrame(data)  # 将JSON数据转换为H2OFrame

    predictions = model.predict(hf)

    # 打印预测结果
    print("预测结果:", predictions.as_data_frame())

    # 返回预测结果作为JSON响应
    response = jsonify(predictions.as_data_frame().to_dict(orient='records'))
    response.headers['Content-Type'] = 'application/json; charset=utf-8'
    return response

if __name__ == '__main__':
    app.run(host='172.168.10.5', port=5000)
