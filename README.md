# Distributed Tracing with Kubernetes, Spring Boot and Jaeger

## Installing

Versions

```bash
# versions

minikube version
#minikube version: v1.33.1
#commit: 5883c09216182566a63dff4c326a6fc9ed2982ff
kubectl version --short
#Client Version: v1.29.2
#Server Version: v1.29.2
docker version --format '{{.Client.Version}} (Client), {{.Server.Version}} (Server)'
# 27.0.3 (Client), 27.0.3 (Server)
```

### 1. Minikube

```zsh
minikube start --driver=docker --cpus 4 --memory 14000 --kubernetes-version=1.29.2 --addons=ingress
kubectl apply --validate=false -f https://github.com/jetstack/cert-manager/releases/download/v1.8.0/cert-manager.yaml
# wait until cert-manager is running
kubectl get all -n cert-manager
```

### 2. Elasticsearch

```zsh
helm repo add elastic https://helm.elastic.co  && helm repo update
kubectl create -f https://download.elastic.co/downloads/eck/2.13.0/crds.yaml
kubectl apply -f https://download.elastic.co/downloads/eck/2.13.0/operator.yaml

# wait until elastic-operator is running
kubectl get pods -n elastic-system -w
# optional: examine logs
kubectl -n elastic-system logs -f statefulset.apps/elastic-operator

cat <<EOF | kubectl apply -f -
apiVersion: elasticsearch.k8s.elastic.co/v1
kind: Elasticsearch
metadata:
  name: quickstart
spec:
  version: 8.14.3
  nodeSets:
  - name: default
    count: 1
    config:
      node.store.allow_mmap: false
  http:
    tls:
      selfSignedCertificate:
        disabled: true
EOF

# wait until quickstart-es-default-0 is running
kubectl get pods -w

# export the generated elasticsearch password
ES_PW=$(kubectl get secret quickstart-es-elastic-user -o go-template='{{.data.elastic | base64decode}}')
echo $ES_PW

# check if es is reachable from a curlpod
kubectl run -i --tty --rm curl-test --image=curlimages/curl --restart=Never --env="ES_PW=$ES_PW" -- sh
echo $ES_PW
curl -u elastic:$ES_PW http://quickstart-es-default:9200/_cluster/health?pretty
exit
```

### 3. Distribute SpringBoot Applications

```zsh
mvn clean package

# use minikube docker deamon
eval $(minikube docker-env)
docker build -t tracing:latest .
# tracing:latest docker image in minikube
docker images | grep tracing

helm install a ./tracing --set downstream=B,applicationName=A
helm install b ./tracing --set downstream=C.D,applicationName=B
helm install c ./tracing --set applicationName=C
helm install d ./tracing --set applicationName=D

kubectl get pods -l app.kubernetes.io/name=tracing && kubectl get svc -l app.kubernetes.io/name=tracing

# TODO: expose service
```

### 4. Deploying Jaeger-Operator and Jaeger-Instances

```zsh
helm install jaeger jaegertracing/jaeger-operator --version 2.53
# wait until jaeger-operator is running
kubectl get pods -w

# optional: check jaeger-operator logs & yaml when not starting
export POD=$(kubectl get pods -l app.kubernetes.io/instance=jaeger -l app.kubernetes.io/name=jaeger-operator --namespace default --output name)
helm show values jaegertracing/jaeger-operator
kubectl logs $POD --namespace default
kubectl get $POD -o yaml

# make sure $ES_PW is available in the terminal
ES_PW=$(kubectl get secret quickstart-es-elastic-user -o go-template='{{.data.elastic | base64decode}}')
echo $ES_PW
# deploy the jaeger instance
cat <<EOF | kubectl apply -f -
apiVersion: jaegertracing.io/v1
kind: Jaeger
metadata:
  name: simple-prod
spec:
  strategy: production
  storage:
    type: elasticsearch
    options:
      es:
        server-urls: http://quickstart-es-default:9200
        username: elastic
        password: $ES_PW
EOF

# wait until jaeger-instance is running
kubectl get pods -w

# validate values for Elasticsearch user, pw and host 
kubectl describe jaeger simple-prod

# access the jaeger-ui http://127.0.0.1:16686/
kubectl describe svc simple-prod-query
kubectl port-forward svc/simple-prod-query 16686:16686 --namespace=default
```

### 6. Generate Traces

```zsh
kubectl port-forward svc/a-tracing 8099:80

# access the microservice-a http://127.0.0.1:8099/generateTrace

# access the jaeger-ui http://127.0.0.1:16686/ and check the generated traces!
```

### 7. Destroy Minikube

```zsh
minikube delete --all
```
