package com.mycompany.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import imports.k8s.*;
import org.cdk8s.App;
import org.cdk8s.Chart;
import org.cdk8s.ChartProps;
import software.constructs.Construct;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Main extends Chart {

    public Main(final Construct scope, final String id, final String appLabel, final String imageTag) {
        this(scope, id, null, appLabel, imageTag);
    }

    public Main(final Construct scope, final String id, final ChartProps props, final String appLabel, final String imageTag) {
        super(scope, id, props);


        Map<String, String> label = Collections.singletonMap("app", appLabel);

        new KubeDeployment(this, "tech-tonic-deployment", KubeDeploymentProps.builder()
                .spec(DeploymentSpec.builder()
                        .replicas(3)
                        .selector(LabelSelector.builder()
                                .matchLabels(label)
                                .build())
                        .template(PodTemplateSpec.builder()
                                .metadata(ObjectMeta.builder().labels(label)
                                        .build())
                                .spec(PodSpec.builder()
                                        .containers(List.of(Container.builder()
                                                .name("app-container-test")
                                                .image(imageTag)
                                                .ports(List.of(ContainerPort.builder()
                                                        .containerPort(80)
                                                        .build()))
                                                .build()))
                                        .build())
                                .build())
                        .build())
                .build());

        // Optionally, define a Service to expose your application
        new KubeService(this, "tech-tonic-service", KubeServiceProps.builder()
                .spec(ServiceSpec.builder()
                        .type("LoadBalancer") // Expose externally
                        .selector(label)
                        .ports(List.of(ServicePort.builder()
                                .port(80)
                                .targetPort(IntOrString.fromNumber(8080))
                                .build()))
                        .build())
                .build());
    }

    public static void main(String[] args) throws IOException {
        final App app = new App();

        ObjectMapper mapper = new ObjectMapper();
        InputStream configStream = Main.class.getClassLoader().getResourceAsStream("image.json");
        if (configStream == null) {
            throw new RuntimeException("config.json not found in resources folder");
        }
        Map<String, String> image = mapper.readValue(configStream, new TypeReference<Map<String, String>>() {});

        String imageTag = image.getOrDefault("image_tag", "nginx:1.19.10"); // Default image
        System.out.println("Using Docker Image: " + imageTag);

        String appLabel = "tech-tonic-label";

        new Main(app, "tech-tonic-id", appLabel, imageTag);
        app.synth();
    }
}
