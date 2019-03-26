## Spring Cloud Kubernetes

[Spring Cloud](https://projects.spring.io/spring-cloud/) integration with [Kubernetes](https://kubernetes.io/)

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.springframework.cloud/spring-cloud-starter-kubernetes/badge.svg?style=flat-square)](https://maven-badges.herokuapp.com/maven-central/org.springframework.cloud/spring-cloud-starter-kubernetes/) 
![Apache 2](https://img.shields.io/badge/license-Apache%202-red.svg)
[![Javadocs](https://www.javadoc.io/badge/org.springframework.cloud/spring-cloud-starter-kubernetes.svg?color=blue)](https://www.javadoc.io/doc/org.springframework.cloud/spring-cloud-starter-kubernetes)


---

#### Version and compatibility

The current version of Spring Cloud Kubernetes is using version 2.2.x of the [Fabric8 Kubernetes Client](https://github.com/fabric8io/kubernetes-client) and is expected to work with version 1.x of [Kubernetes](https://github.com/kubernetes/kubernetes) and 1.x of [Openshift](https://github.com/openshift/origin).
Note, that [Kubernetes](https://github.com/kubernetes/kubernetes) and [Openshift](https://github.com/openshift/origin) are for the most part backwards compatible so, its expected that this framework is compatible with both the latest and earlier versions.


### Building

You can just use maven to build it from sources:

```
mvn clean install
```    

### Usage

The project provides a "starter" module, so you just need to add the following dependency in your project.

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-kubernetes</artifactId>
    <version>x.y.z</version>
</dependency>
```    
