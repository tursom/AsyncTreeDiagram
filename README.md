<h1>全异步TreeDiagram服务器</h1>

<h3>使用docker运行</h3>

构建docker镜像

<code>
wget https://github.com/tursom/AsyncTreeDiagram/releases/download/1.0/AsyncTreeDiagramDocker.tar.gz
tar xvf AsyncTreeDiagramDocker.tar.gz
cd AsyncTreeDiagram-0.2
docker build -t async-tree-diagram .
</code>

运行docker镜像

<code>
docker run -it --rm -p 12345:12345 --name my-running-app async-tree-diagram
</code>
