# Python 扩展简单方案

## 目录
1. [现有推荐算法 vs Python 推荐算法对比](#1-现有推荐算法-vs-python-推荐算法对比)
2. [语义搜索增强](#2-语义搜索增强)
3. [用户行为分析](#3-用户行为分析)
4. [内容审核增强](#4-内容审核增强)
5. [图像处理](#5-图像处理)
6. [推荐系统优化](#6-推荐系统优化)
7. [知识图谱构建](#7-知识图谱构建)

---

## 1. 现有推荐算法 vs Python 推荐算法对比

### 现有 Java 实现（RecommendationServiceImpl.java）

**算法类型**：基于用户的协同过滤（User-Based CF）

**实现方式**：
- 使用余弦相似度计算用户相似性
- 构建用户-内容评分矩阵（收藏5分、点赞3分、浏览1分）
- 综合协同过滤分数和兴趣推荐分数
- 最多参考20个相似用户

**优势**：
- 与 Spring Boot 后端集成良好
- 无需额外依赖
- 实时计算，无延迟
- 维护简单

**劣势**：
- 算法简单，仅使用协同过滤
- 无法理解诗词内容语义
- 冷启动问题（新用户/新内容）
- 计算复杂度高（O(n²)）

### Python 推荐算法方案

**算法类型**：深度学习推荐 + 语义理解

**实现方式**：
- 使用 Transformers 进行诗词语义理解
- 使用深度学习模型（DeepFM/Wide&Deep）进行推荐
- 结合协同过滤和内容推荐

**优势**：
- 理解诗词内容语义
- 更精准的个性化推荐
- 解决冷启动问题
- 支持更复杂的特征工程

**劣势**：
- 需要额外的 Python 服务
- 模型训练和部署复杂
- 需要 GPU 资源
- 维护成本高

### 对比总结

| 维度 | Java 实现 | Python 实现 |
|------|----------|------------|
| **算法复杂度** | 简单（协同过滤） | 复杂（深度学习） |
| **语义理解** | 无 | 强 |
| **冷启动** | 差 | 好 |
| **计算效率** | 高 | 中 |
| **维护成本** | 低 | 高 |
| **推荐精度** | 中 | 高 |
| **扩展性** | 差 | 好 |

**建议**：保留现有 Java 实现作为基础，Python 服务作为增强，通过 API 调用结合使用。

---

## 2. 语义搜索增强

### 功能说明
基于语义理解的诗词搜索，支持"找相似意境的诗词"等高级搜索。

### 技术栈
- Python 3.9+
- Sentence-Transformers（语义向量化）
- FAISS（向量索引）
- FastAPI（API 服务）

### 实现步骤

```python
# 1. 安装依赖
pip install sentence-transformers faiss-cpu fastapi uvicorn

# 2. 语义向量化服务
from sentence_transformers import SentenceTransformer
import faiss
import numpy as np

class SemanticSearch:
    def __init__(self):
        self.model = SentenceTransformer('paraphrase-multilingual-MiniLM-L12-v2')
        self.index = None
        self.poems = []
    
    def build_index(self, poems):
        """构建向量索引"""
        self.poems = poems
        texts = [f"{p['title']} {p['content']}" for p in poems]
        embeddings = self.model.encode(texts)
        
        dimension = embeddings.shape[1]
        self.index = faiss.IndexFlatL2(dimension)
        self.index.add(embeddings.astype('float32'))
    
    def search(self, query, top_k=10):
        """语义搜索"""
        query_embedding = self.model.encode([query])
        distances, indices = self.index.search(query_embedding.astype('float32'), top_k)
        
        results = []
        for i, idx in enumerate(indices[0]):
            results.append({
                'poem': self.poems[idx],
                'score': float(1 / (1 + distances[0][i]))
            })
        return results

# 3. FastAPI 服务
from fastapi import FastAPI
app = FastAPI()
search_engine = SemanticSearch()

@app.on_event("startup")
async def startup():
    # 从数据库加载诗词数据
    poems = load_poems_from_db()
    search_engine.build_index(poems)

@app.get("/api/semantic-search")
async def semantic_search(keyword: str, limit: int = 10):
    return search_engine.search(keyword, limit)
```

### 集成方式
Spring Boot 后端通过 RestTemplate 调用 Python 语义搜索服务。

---

## 3. 用户行为分析

### 功能说明
分析用户浏览、收藏、点赞行为，优化推荐算法。

### 技术栈
- Python 3.9+
- Pandas（数据处理）
- Scikit-learn（机器学习）
- Matplotlib/Seaborn（可视化）

### 实现步骤

```python
# 1. 安装依赖
pip install pandas scikit-learn matplotlib seaborn pymysql

# 2. 用户行为分析服务
import pandas as pd
from sklearn.cluster import KMeans
from sklearn.preprocessing import StandardScaler

class UserBehaviorAnalyzer:
    def __init__(self, db_config):
        self.db_config = db_config
    
    def load_user_behavior(self):
        """加载用户行为数据"""
        import pymysql
        conn = pymysql.connect(**self.db_config)
        
        # 加载收藏数据
        favorites = pd.read_sql("SELECT * FROM user_favorite", conn)
        
        # 加载点赞数据
        likes = pd.read_sql("SELECT * FROM user_like", conn)
        
        # 加载浏览历史
        history = pd.read_sql("SELECT * FROM user_history", conn)
        
        conn.close()
        return favorites, likes, history
    
    def build_user_profiles(self, favorites, likes, history):
        """构建用户画像"""
        # 合并行为数据
        behaviors = pd.concat([
            favorites[['user_id', 'target_id']].assign(action='favorite'),
            likes[['user_id', 'target_id']].assign(action='like'),
            history[['user_id', 'target_id']].assign(action='view')
        ])
        
        # 构建用户-行为矩阵
        user_behavior_matrix = behaviors.pivot_table(
            index='user_id', 
            columns='target_id', 
            values='action', 
            aggfunc='count',
            fill_value=0
        )
        
        return user_behavior_matrix
    
    def cluster_users(self, user_behavior_matrix, n_clusters=5):
        """用户聚类"""
        scaler = StandardScaler()
        scaled_data = scaler.fit_transform(user_behavior_matrix)
        
        kmeans = KMeans(n_clusters=n_clusters, random_state=42)
        clusters = kmeans.fit_predict(scaled_data)
        
        return clusters
    
    def analyze(self):
        """执行分析"""
        favorites, likes, history = self.load_user_behavior()
        user_behavior_matrix = self.build_user_profiles(favorites, likes, history)
        clusters = self.cluster_users(user_behavior_matrix)
        
        return {
            'user_clusters': clusters,
            'user_behavior_matrix': user_behavior_matrix
        }
```

### 应用场景
- 用户分群：识别不同兴趣群体
- 行为预测：预测用户可能喜欢的诗词
- 推荐优化：基于用户画像优化推荐算法

---

## 4. 内容审核增强

### 功能说明
自动检测诗词内容中的敏感词、违规内容。

### 技术栈
- Python 3.9+
- 正则表达式
- 机器学习（可选）

### 实现步骤

```python
# 1. 安装依赖
pip install fastapi uvicorn

# 2. 内容审核服务
import re
from typing import List, Dict

class ContentModerator:
    def __init__(self):
        # 敏感词库
        self.sensitive_words = self.load_sensitive_words()
        
        # 违规模式
        self.violation_patterns = [
            r'政治敏感词1',
            r'政治敏感词2',
            r'暴力词汇',
            # ... 更多模式
        ]
    
    def load_sensitive_words(self):
        """加载敏感词库"""
        # 从文件或数据库加载
        return ['敏感词1', '敏感词2', '敏感词3']
    
    def check_content(self, content: str) -> Dict:
        """检查内容"""
        results = {
            'is_safe': True,
            'violations': [],
            'suggestions': []
        }
        
        # 检查敏感词
        for word in self.sensitive_words:
            if word in content:
                results['is_safe'] = False
                results['violations'].append({
                    'type': 'sensitive_word',
                    'word': word,
                    'suggestion': f'建议替换"{word}"'
                })
        
        # 检查违规模式
        for pattern in self.violation_patterns:
            matches = re.findall(pattern, content)
            if matches:
                results['is_safe'] = False
                results['violations'].append({
                    'type': 'violation_pattern',
                    'matches': matches,
                    'suggestion': '内容包含违规信息'
                })
        
        return results

# 3. FastAPI 服务
from fastapi import FastAPI
app = FastAPI()
moderator = ContentModerator()

@app.post("/api/content-moderation")
async def moderate_content(content: str):
    return moderator.check_content(content)
```

### 集成方式
- 用户发布诗词时，调用审核服务
- 审核通过后才允许发布
- 提供审核建议，帮助用户修改内容

---

## 5. 图像处理

### 5.1 诗词配图生成

#### 功能说明
根据诗词内容自动生成水墨风格配图。

#### 技术栈
- Python 3.9+
- Stable Diffusion（AI 图像生成）
- Pillow（图像处理）

#### 实现步骤

```python
# 1. 安装依赖
pip install diffusers transformers torch pillow

# 2. 诗词配图生成服务
from diffusers import StableDiffusionPipeline
import torch
from PIL import Image

class PoemImageGenerator:
    def __init__(self):
        self.pipe = StableDiffusionPipeline.from_pretrained(
            "runwayml/stable-diffusion-v1-5",
            torch_dtype=torch.float16
        )
        self.pipe = self.pipe.to("cuda")
    
    def generate_image(self, poem_content: str, style: str = "水墨") -> Image:
        """根据诗词生成配图"""
        # 构建提示词
        prompt = f"{style}风格，{poem_content}，中国传统艺术，高质量"
        
        # 生成图像
        image = self.pipe(prompt).images[0]
        
        return image
    
    def add_text_watermark(self, image: Image, text: str) -> Image:
        """添加文字水印"""
        from PIL import ImageDraw, ImageFont
        
        draw = ImageDraw.Draw(image)
        font = ImageFont.truetype("path/to/font.ttf", 36)
        
        # 在右下角添加水印
        position = (image.width - 200, image.height - 50)
        draw.text(position, text, fill=(255, 255, 255, 128), font=font)
        
        return image

# 3. FastAPI 服务
from fastapi import FastAPI
from fastapi.responses import StreamingResponse
import io

app = FastAPI()
generator = PoemImageGenerator()

@app.get("/api/generate-poem-image")
async def generate_poem_image(poem_content: str, style: str = "水墨"):
    image = generator.generate_image(poem_content, style)
    
    # 转换为字节流
    img_byte_arr = io.BytesIO()
    image.save(img_byte_arr, format='PNG')
    img_byte_arr.seek(0)
    
    return StreamingResponse(img_byte_arr, media_type="image/png")
```

### 5.2 图像内容识别

#### 功能说明
识别用户上传图片中的内容，辅助看图写诗。

#### 技术栈
- Python 3.9+
- OpenCV（图像处理）
- YOLO（物体检测）
- CLIP（图像理解）

#### 实现步骤

```python
# 1. 安装依赖
pip install opencv-python ultralytics transformers torch

# 2. 图像内容识别服务
import cv2
from ultralytics import YOLO
from transformers import CLIPProcessor, CLIPModel

class ImageAnalyzer:
    def __init__(self):
        self.yolo_model = YOLO('yolov8n.pt')
        self.clip_model = CLIPModel.from_pretrained("openai/clip-vit-base-patch32")
        self.clip_processor = CLIPProcessor.from_pretrained("openai/clip-vit-base-patch32")
    
    def detect_objects(self, image_path: str) -> List[Dict]:
        """检测图像中的物体"""
        results = self.yolo_model(image_path)
        
        objects = []
        for result in results:
            for box in result.boxes:
                objects.append({
                    'class': result.names[int(box.cls)],
                    'confidence': float(box.conf),
                    'bbox': box.xyxy[0].tolist()
                })
        
        return objects
    
    def analyze_scene(self, image_path: str) -> str:
        """分析图像场景"""
        image = Image.open(image_path)
        
        # 使用 CLIP 分析场景
        inputs = self.clip_processor(
            text=["山水", "花鸟", "人物", "建筑", "自然"],
            images=image,
            return_tensors="pt",
            padding=True
        )
        
        outputs = self.clip_model(**inputs)
        logits = outputs.logits_per_image[0]
        probs = logits.softmax(dim=-1)
        
        # 返回最可能的场景
        scene_labels = ["山水", "花鸟", "人物", "建筑", "自然"]
        return scene_labels[probs.argmax()]
    
    def generate_poem_prompt(self, image_path: str) -> str:
        """根据图像生成写诗提示"""
        objects = self.detect_objects(image_path)
        scene = self.analyze_scene(image_path)
        
        prompt = f"场景：{scene}，"
        prompt += "包含：" + "、".join([obj['class'] for obj in objects[:5]])
        
        return prompt

# 3. FastAPI 服务
from fastapi import FastAPI, UploadFile, File

app = FastAPI()
analyzer = ImageAnalyzer()

@app.post("/api/analyze-image")
async def analyze_image(file: UploadFile = File(...)):
    # 保存上传的图片
    with open(f"temp/{file.filename}", "wb") as buffer:
        buffer.write(await file.read())
    
    # 分析图像
    prompt = analyzer.generate_poem_prompt(f"temp/{file.filename}")
    
    return {"prompt": prompt}
```

---

## 6. 推荐系统优化

### 功能说明
使用深度学习模型优化推荐算法。

### 技术栈
- Python 3.9+
- TensorFlow/PyTorch
- DeepFM/Wide&Deep 模型

### 实现步骤

```python
# 1. 安装依赖
pip install tensorflow pandas numpy scikit-learn

# 2. 深度学习推荐模型
import tensorflow as tf
from tensorflow.keras.layers import Input, Dense, Concatenate, Embedding, Flatten
from tensorflow.keras.models import Model
import pandas as pd
import numpy as np

class DeepRecommender:
    def __init__(self, num_users, num_items, embedding_dim=32):
        self.num_users = num_users
        self.num_items = num_items
        self.embedding_dim = embedding_dim
        self.model = self.build_model()
    
    def build_model(self):
        """构建 Wide&Deep 模型"""
        # 输入层
        user_input = Input(shape=(1,), name='user_id')
        item_input = Input(shape=(1,), name='item_id')
        
        # Embedding 层
        user_embedding = Embedding(self.num_users, self.embedding_dim)(user_input)
        item_embedding = Embedding(self.num_items, self.embedding_dim)(item_input)
        
        user_flat = Flatten()(user_embedding)
        item_flat = Flatten()(item_embedding)
        
        # Wide 部分（线性模型）
        wide = Concatenate()([user_flat, item_flat])
        wide_output = Dense(1, activation='sigmoid')(wide)
        
        # Deep 部分（深度神经网络）
        deep = Concatenate()([user_flat, item_flat])
        deep = Dense(128, activation='relu')(deep)
        deep = Dense(64, activation='relu')(deep)
        deep_output = Dense(1, activation='sigmoid')(deep)
        
        # 合并 Wide 和 Deep
        output = Concatenate()([wide_output, deep_output])
        output = Dense(1, activation='sigmoid')(output)
        
        model = Model(inputs=[user_input, item_input], outputs=output)
        model.compile(optimizer='adam', loss='binary_crossentropy', metrics=['accuracy'])
        
        return model
    
    def train(self, interactions):
        """训练模型"""
        users = interactions['user_id'].values
        items = interactions['item_id'].values
        labels = interactions['label'].values
        
        self.model.fit(
            [users, items],
            labels,
            epochs=10,
            batch_size=32,
            validation_split=0.2
        )
    
    def predict(self, user_id, item_ids):
        """预测用户对物品的评分"""
        user_ids = np.array([user_id] * len(item_ids))
        predictions = self.model.predict([user_ids, item_ids])
        return predictions.flatten()

# 3. FastAPI 服务
from fastapi import FastAPI

app = FastAPI()
recommender = DeepRecommender(num_users=10000, num_items=50000)

@app.get("/api/deep-recommend")
async def deep_recommend(user_id: int, limit: int = 10):
    # 获取候选物品
    candidate_items = get_candidate_items(user_id)
    
    # 预测评分
    scores = recommender.predict(user_id, candidate_items)
    
    # 返回推荐结果
    top_items = candidate_items[np.argsort(scores)[-limit:][::-1]]
    
    return {"user_id": user_id, "recommended_items": top_items.tolist()}
```

---

## 7. 知识图谱构建

### 功能说明
构建诗词知识图谱，支持智能问答和推荐。

### 技术栈
- Python 3.9+
- Neo4j（图数据库）
- NetworkX（图分析）

### 实现步骤

```python
# 1. 安装依赖
pip install neo4j networkx py2neo

# 2. 知识图谱构建服务
from neo4j import GraphDatabase
import networkx as nx

class PoetryKnowledgeGraph:
    def __init__(self, uri, user, password):
        self.driver = GraphDatabase.driver(uri, auth=(user, password))
        self.G = nx.DiGraph()
    
    def close(self):
        self.driver.close()
    
    def create_poet_node(self, poet_id, name, dynasty):
        """创建诗人节点"""
        with self.driver.session() as session:
            session.run(
                "MERGE (p:Poet {id: $id, name: $name, dynasty: $dynasty})",
                id=poet_id, name=name, dynasty=dynasty
            )
    
    def create_poem_node(self, poem_id, title, content):
        """创建诗词节点"""
        with self.driver.session() as session:
            session.run(
                "MERGE (p:Poem {id: $id, title: $title, content: $content})",
                id=poem_id, title=title, content=content
            )
    
    def create_relationship(self, poet_id, poem_id, relationship_type):
        """创建关系"""
        with self.driver.session() as session:
            session.run(
                f"""
                MATCH (p:Poet {{id: $poet_id}})
                MATCH (poem:Poem {{id: $poem_id}})
                MERGE (p)-[:{relationship_type}]->(poem)
                """,
                poet_id=poet_id, poem_id=poem_id
            )
    
    def query_related_poems(self, poet_name):
        """查询诗人的所有作品"""
        with self.driver.session() as session:
            result = session.run(
                """
                MATCH (p:Poet {name: $name})-[:WROTE]->(poem:Poem)
                RETURN poem.title, poem.content
                """,
                name=poet_name
            )
            return [record.data() for record in result]
    
    def query_similar_poets(self, poet_name, limit=5):
        """查询相似诗人（基于共同风格或朝代）"""
        with self.driver.session() as session:
            result = session.run(
                """
                MATCH (p:Poet {name: $name})-[:WROTE]->(poem:Poem)
                MATCH (other:Poet)-[:WROTE]->(poem)
                WHERE other.name <> $name
                RETURN other.name, COUNT(poem) as common_poems
                ORDER BY common_poems DESC
                LIMIT $limit
                """,
                name=poet_name, limit=limit
            )
            return [record.data() for record in result]
    
    def build_networkx_graph(self):
        """构建 NetworkX 图用于分析"""
        with self.driver.session() as session:
            # 获取所有节点和关系
            result = session.run(
                """
                MATCH (p:Poet)-[r]->(poem:Poem)
                RETURN p.name, type(r), poem.title
                """
            )
            
            for record in result:
                poet_name = record[0]
                rel_type = record[1]
                poem_title = record[2]
                
                self.G.add_edge(poet_name, poem_title, relationship=rel_type)
    
    def find_influential_poets(self):
        """找出最有影响力的诗人"""
        self.build_networkx_graph()
        
        # 计算度中心性
        degree_centrality = nx.degree_centrality(self.G)
        
        # 排序
        sorted_poets = sorted(
            degree_centrality.items(),
            key=lambda x: x[1],
            reverse=True
        )
        
        return sorted_poets[:10]

# 3. FastAPI 服务
from fastapi import FastAPI

app = FastAPI()
kg = PoetryKnowledgeGraph("bolt://localhost:7687", "neo4j", "password")

@app.get("/api/knowledge-graph/poet/{name}")
async def get_poet_info(name: str):
    poems = kg.query_related_poems(name)
    similar_poets = kg.query_similar_poets(name)
    
    return {
        "poet": name,
        "poems": poems,
        "similar_poets": similar_poets
    }

@app.get("/api/knowledge-graph/influential-poets")
async def get_influential_poets():
    return kg.find_influential_poets()
```

### 应用场景
- 智能问答："李白写了哪些关于月亮的诗？"
- 关联推荐："喜欢苏轼的用户也喜欢..."
- 知识发现：发现诗人之间的关系网络

---

## 总结

以上方案涵盖了语义搜索、用户行为分析、内容审核、图像处理、推荐系统优化和知识图谱构建六个方向。

### 实施优先级建议

1. **高优先级**（立即实施）
   - 语义搜索增强
   - 内容审核增强

2. **中优先级**（短期实施）
   - 用户行为分析
   - 推荐系统优化

3. **低优先级**（长期实施）
   - 图像处理
   - 知识图谱构建

### 技术架构建议

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Vue 3 前端    │    │  Spring Boot    │    │   Python 服务   │
│                 │◄──►│                 │◄──►│                 │
│  用户界面       │    │  业务逻辑       │    │  AI/NLP 处理    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │                        │
                              ▼                        ▼
                       ┌─────────────────┐    ┌─────────────────┐
                       │   MySQL 数据库  │    │  Python 模型    │
                       │                 │    │                 │
                       │  业务数据       │    │  训练数据       │
                       └─────────────────┘    └─────────────────┘
```

Python 服务作为独立微服务部署，通过 REST API 与 Spring Boot 后端通信，使用 Docker 容器化部署。