ShapeFile原理

### 📂 Shapefile 文件结构

> 一个简单的ShpeFile文件结构

![image-20251202171835639](https://picgo-liziyuan.oss-cn-hangzhou.aliyuncs.com/zhd202512021718713.png)

Shapefile 是一种常见的文件格式，包含许多相同类型的特征（Feature）。每个 Shapefile 只有一个特征类型 (FeatureType)。

Shapefile 的 .shp 文件存储的是 同一类型的 Feature，例如：

- 点 (Point)
- 线 (LineString)
- 面/多边形 (Polygon / MultiPolygon)

#### 经典的三大文件：

- filename.shp: 存储几何形状 (shapes),例如它是点、线还是面特征。。
- filename.shx: 形状到属性的索引 (shapes to attributes index)。
- filename.dbf: 属性数据 (attributes),是数据库文件，存储在属性表中查看的数据。

#### 基本元数据：

- filename.prj: 存储投影信息 (projection),用于正确地将 Shapefile 定位在地球表面。

#### 开源扩展（Open source extensions）：

- filename.qix: 四叉树空间索引 (quadtree spatial index)。
- filename.fix: 要素 ID 索引 (feature id index)。
- filename.sld: 样式化图层描述符 (Styled Layer Descriptor) 样式 XML 对象。

#### ESRI 专有扩展（GeoTools 忽略）：

- filename.sbn: 属性索引 (attribute index)。
- filename.sbx: 空间索引 (spatial index)。
- filename.lyr: 仅 ArcMap 可用的样式对象。
- filename.avl: ArcView 样式对象。
- filename.shp.xml: FGDC 元数据。

这种文件格式（诞生于很久以前）被称为 “边车文件” (sidecar files) 格式。最少需要 filename.shp 文件及其边车文件 filename.dbf。

如果 DataStore 仅用于读取，文件可以被 gzip 压缩，并通过额外的文件名扩展名 .gz 标记。

如果 shp 或 shp.gz 文件缺失，GeoTools 仍会提供不带几何形状的要素。因此，只需要 dbf 或 dbf.gz 文件存在即可。提供的 URL 可以以 shp、shp.gz、dbf 或 dbf.gz 结尾。



**只有  .shp  是入口文件**