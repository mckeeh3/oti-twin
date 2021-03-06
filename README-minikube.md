
# Minikube Installation and Setup

Follow these instructions for installing and running the woe-twin microservice using Minikube.

## Prerequisites

Clone the weo-sim Github project.

~~~bash
git clone https://github.com/mckeeh3/woe-twin.git
~~~

## Install Kubernetes CLI

Follow the instructions in the [Kubernetes documentation](https://kubernetes.io/docs/tasks/tools/#kubectl) new tab to install `kubectl`.

The `kubectl` CLI provides a nice Kubectl Autocomplete feature for `bash` and `zsh`.
See the [kubectl Cheat Sheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/#kubectl-autocomplete) for instructions.

Also, consider installing [`kubectx`](https://github.com/ahmetb/kubectx), which also includes `kubens`.

Mac:

~~~bash
brew install kubectx
~~~

Arch Linux:

~~~bash
yay kubectx
~~~

## Install Minikube

Follow the [instructions](https://kubernetes.io/docs/tasks/tools/install-minikube/) for installing Minikube.

### Start Minikube

You may want to allocate more CPU and memory capacity to run the WoW application than the defaults. There are two `minikube` command options available for adjusting the CPU and memory allocation settings.

~~~bash
minikube start --driver=virtualbox --cpus=C --memory=M
~~~

For example, allocate 4 CPUs and 10 gig of memory.

~~~bash
minikube start --driver=virtualbox --cpus=4 --memory=10g
~~~

~~~text
😄  minikube v1.19.0 on Arch 
🎉  minikube 1.20.0 is available! Download it: https://github.com/kubernetes/minikube/releases/tag/v1.20.0
💡  To disable this notice, run: 'minikube config set WantUpdateNotification false'

✨  Automatically selected the docker driver. Other choices: virtualbox, ssh
❗  Your cgroup does not allow setting memory.
    ▪ More information: https://docs.docker.com/engine/install/linux-postinstall/#your-kernel-does-not-support-cgroup-swap-limit-capabilities
👍  Starting control plane node minikube in cluster minikube
🚜  Pulling base image ...
💾  Downloading Kubernetes v1.20.2 preload ...
    > preloaded-images-k8s-v10-v1...: 491.71 MiB / 491.71 MiB  100.00% 39.83 Mi
    > gcr.io/k8s-minikube/kicbase...: 357.67 MiB / 357.67 MiB  100.00% 7.98 MiB
🔥  Creating docker container (CPUs=8, Memory=20480MB) ...
🐳  Preparing Kubernetes v1.20.2 on Docker 20.10.5 ...
    ▪ Generating certificates and keys ...
    ▪ Booting up control plane ...
    ▪ Configuring RBAC rules ...
🔎  Verifying Kubernetes components...
    ▪ Using image gcr.io/k8s-minikube/storage-provisioner:v5
🌟  Enabled addons: storage-provisioner, default-storageclass
🏄  Done! kubectl is now configured to use "minikube" cluster and "default" namespace by default
~~~

Once the `minikube` Kubernetes cluster is up you can check its status using the following commands.

~~~bash
kubectl get events
~~~

~~~text
LAST SEEN   TYPE     REASON                    OBJECT          MESSAGE
17s         Normal   Starting                  node/minikube   Starting kubelet.
17s         Normal   NodeHasSufficientMemory   node/minikube   Node minikube status is now: NodeHasSufficientMemory
17s         Normal   NodeHasNoDiskPressure     node/minikube   Node minikube status is now: NodeHasNoDiskPressure
17s         Normal   NodeHasSufficientPID      node/minikube   Node minikube status is now: NodeHasSufficientPID
17s         Normal   NodeAllocatableEnforced   node/minikube   Updated Node Allocatable limit across pods
17s         Normal   NodeReady                 node/minikube   Node minikube status is now: NodeReady
8s          Normal   RegisteredNode            node/minikube   Node minikube event: Registered Node minikube in Controller
~~~

~~~bash
kubectl get all -A
~~~

~~~text
kube-system   pod/coredns-74ff55c5b-g6c2d            0/1     Running   0          20s
kube-system   pod/etcd-minikube                      0/1     Running   0          29s
kube-system   pod/kube-apiserver-minikube            1/1     Running   0          29s
kube-system   pod/kube-controller-manager-minikube   0/1     Running   0          29s
kube-system   pod/kube-proxy-rzrd2                   0/1     Error     2          20s
kube-system   pod/kube-scheduler-minikube            0/1     Running   0          29s
kube-system   pod/storage-provisioner                1/1     Running   0          35s

NAMESPACE     NAME                 TYPE        CLUSTER-IP   EXTERNAL-IP   PORT(S)                  AGE
default       service/kubernetes   ClusterIP   10.96.0.1    <none>        443/TCP                  37s
kube-system   service/kube-dns     ClusterIP   10.96.0.10   <none>        53/UDP,53/TCP,9153/TCP   36s

NAMESPACE     NAME                        DESIRED   CURRENT   READY   UP-TO-DATE   AVAILABLE   NODE SELECTOR            AGE
kube-system   daemonset.apps/kube-proxy   1         1         0       1            0           kubernetes.io/os=linux   36s

NAMESPACE     NAME                      READY   UP-TO-DATE   AVAILABLE   AGE
kube-system   deployment.apps/coredns   0/1     1            0           36s

NAMESPACE     NAME                                DESIRED   CURRENT   READY   AGE
kube-system   replicaset.apps/coredns-74ff55c5b   1         1         0       20s
~~~

### Create the Kubernetes namespace

The namespace only needs to be created once.

~~~bash
kubectl create namespace woe-twin
~~~

~~~text
namespace/woe-twin created
~~~

Set this namespace as the default for subsequent `kubectl` commands.

~~~bash
kubectl config set-context --current --namespace=woe-twin
~~~

~~~text
Context "minikube" modified.
~~~

## Deploy either Cassandra or PostgreSQL database

See the instructions for deploying to Kubernetes either
[Cassandra](https://github.com/mckeeh3/woe-twin/blob/master/README-helm-cassandra.md) or
[PostgreSQL](https://github.com/mckeeh3/woe-twin/blob/master/README-helm-postgresql.md).

### Adjust application.conf

Edit the `application.conf` file as follows. Add the database configuration for the specific Akka Persistence event journal database.

For Cassandra, add the following line.

~~~text
include "application-helm-cassandra"
~~~

For PostgreSQL, add the following line.

~~~text
include "application-helm-postgresql"
~~~

### Adjust the pom fabric8 plugin for the specific Docker repository

When using Docker hub, add your Docker user to the image name in the pom.

~~~text
      <plugin>
        <!-- For latest version see - https://dmp.fabric8.io/ -->
        <groupId>io.fabric8</groupId>
        <artifactId>docker-maven-plugin</artifactId>
        <version>0.36.0</version>
        <configuration>
          <images>
            <image>
              <!-- Modify as needed for the target repo. For Docker hub use "your-docker-user"/%a -->
              <name>mckeeh3/%a</name>
~~~

### Build the Docker image

From the woe-twin project directory.

~~~bash
mvn clean package docker:push
~~~

~~~text
...

[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  55.361 s
[INFO] Finished at: 2021-05-24T20:38:20-04:00
[INFO] ------------------------------------------------------------------------
~~~

### Deploy the Docker images to the Kubernetes cluster

Select the deployment file for the database environment that you are using.

For Cassandra.

~~~bash
kubectl apply -f kubernetes/woe-twin-helm-cassandra.yml
~~~

For PostgreSQL.

~~~bash
kubectl apply -f kubernetes/woe-twin-helm-postgresql.yml
~~~

~~~text
deployment.apps/woe-twin created
role.rbac.authorization.k8s.io/pod-reader created
rolebinding.rbac.authorization.k8s.io/read-pods created
~~~

### Verify that the pods are running

This may take a few moments.

~~~bash
kubectl get pods
~~~

~~~text
NAME                        READY   STATUS    RESTARTS   AGE
woe-twin-7594dcc7b7-bmf4t   1/1     Running   0          3m21s
woe-twin-7594dcc7b7-gnfld   1/1     Running   0          3m21s
woe-twin-7594dcc7b7-j9hv9   1/1     Running   0          3m21s
~~~

If there is a problem check the logs.

~~~bash
kubectl logs woe-twin-7594dcc7b7-bmf4t
~~~

You can examine one of the pods in more detail, e.g. examine the environment variable settings.

~~~bash
kubectl describe pod woe-twin-7594dcc7b7-bmf4t
~~~

If there are configuration issues or if you want to check something in a container, start a `bash` shell in one of the pods using the following command. For example, start a `bash` shell on the 3rd pod listed above.

~~~bash
kubectl exec -it woe-twin-77dfcc864b-vf78s -- /bin/bash
~~~

~~~text
root@woe-twin-77dfcc864b-vf78s:/# env | grep woe
HOSTNAME=woe-twin-77dfcc864b-vf78s
woe_twin_http_server_port=8080
NAMESPACE=woe-twin
woe_simulator_http_server_port=8080
woe_simulator_http_server_host=woe-twin-service.woe-twin.svc.cluster.local
woe_twin_http_server_host=woe-twin-service.woe-twin.svc.cluster.local
woe_twin_telemetry_servers=woe.simulator.GrpcClient:woe-twin-service.woe-twin.svc.cluster.local:8081
root@woe-twin-77dfcc864b-vf78s:/# exit
~~~

### Create a Load Balancer to enable external access

Create a load balancer to enable access to the WOE Sim microservice HTTP endpoint.

~~~bash
kubectl expose deployment woe-twin --type=LoadBalancer --name=woe-twin-service
~~~

~~~text
service/woe-twin-service exposed
~~~

Next, view to external port assignments.

~~~bash
kubectl get services woe-twin-service
~~~

~~~text
NAME              TYPE           CLUSTER-IP      EXTERNAL-IP   PORT(S)                                        AGE
woe-twin-service   LoadBalancer   10.107.51.103   <pending>     2552:32361/TCP,8558:31809/TCP,8080:30968/TCP   108s
~~~

Note that in this example, the Kubernetes internal port 8558 external port assignment of 31809.

For MiniKube deployments, the full URL to access the HTTP endpoint is constructed using the MiniKube IP and the external port.

~~~bash
minikube ip
~~~

~~~text
192.168.99.102
~~~

In this example the MiniKube IP is: `192.168.99.102`

Try accessing this load balancer endpoint using the curl command or from a browser.

~~~bash
curl -v http://$(minikube ip):31809/cluster/members | python -m json.tool
~~~

~~~text
*   Trying 192.168.99.102:31809...
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0* Connected to 192.168.99.102 (192.168.99.102) port 31809 (#0)
> GET /cluster/members HTTP/1.1
> Host: 192.168.99.102:31809
> User-Agent: curl/7.70.0
> Accept: */*
>
* Mark bundle as not supporting multiuse
< HTTP/1.1 200 OK
< Server: akka-http/10.1.12
< Date: Fri, 19 Jun 2020 17:46:13 GMT
< Content-Type: application/json
< Content-Length: 570
<
{ [570 bytes data]
100   570  100   570    0     0   6867      0 --:--:-- --:--:-- --:--:--  6867
* Connection #0 to host 192.168.99.102 left intact
{
    "leader": "akka://woe-twin@172.17.0.11:25520",
    "members": [
        {
            "node": "akka://woe-twin@172.17.0.11:25520",
            "nodeUid": "7176760119283282430",
            "roles": [
                "dc-default"
            ],
            "status": "Up"
        },
        {
            "node": "akka://woe-twin@172.17.0.12:25520",
            "nodeUid": "6695287075719844052",
            "roles": [
                "dc-default"
            ],
            "status": "Up"
        },
        {
            "node": "akka://woe-twin@172.17.0.13:25520",
            "nodeUid": "-7478917548710968969",
            "roles": [
                "dc-default"
            ],
            "status": "Up"
        }
    ],
    "oldest": "akka://woe-twin@172.17.0.11:25520",
    "oldestPerRole": {
        "dc-default": "akka://woe-twin@172.17.0.11:25520"
    },
    "selfNode": "akka://woe-twin@172.17.0.12:25520",
    "unreachable": []
}
~~~

Next, deploy the [woe-twin microservice](https://github.com/mckeeh3/woe-twin).
