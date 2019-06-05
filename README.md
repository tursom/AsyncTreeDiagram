全异步TreeDiagram服务器
===

### 使用docker运行

构建docker镜像
```sh
docker build -t async-tree-diagram https://github.com/tursom/AsyncTreeDiagram/releases/download/1.0/AsyncTreeDiagramDocker.tar.gz
```
运行docker镜像
```sh
docker run -p 12345:12345 -v upload:/www/upload -v TreeDiagram.db:/www/TreeDiagram.db -v log:/www/log async-tree-diagram
```