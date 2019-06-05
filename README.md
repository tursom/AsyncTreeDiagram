全异步TreeDiagram服务器
===

### 使用docker运行

构建docker镜像
```sh
docker build -t async-tree-diagram https://github.com/tursom/AsyncTreeDiagram/releases/download/1.0/AsyncTreeDiagramDocker.tar.gz
```
运行docker镜像
```sh
docker run -it --rm -p 12345:12345 --name my-running-app async-tree-diagram
```