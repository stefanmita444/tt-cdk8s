package com.mycompany.app;

import software.constructs.Construct;
import org.cdk8s.App;
import org.cdk8s.Chart;
import org.cdk8s.ChartProps;
import imports.k8s.*;

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

        new KubeDeployment(this, "my-deployment", KubeDeploymentProps.builder()
                .spec(DeploymentSpec.builder()
                        .replicas(3)
                        .selector(LabelSelector.builder()
                                .matchLabels(label)
                                .build())
                        .template(PodTemplateSpec.builder()
                                .metadata(ObjectMeta.builder()
                                        .labels(label)
                                        .build())
                                .spec(PodSpec.builder()
                                        .containers(List.of(Container.builder()
                                                .name("app-container")
                                                .image(imageTag) // Use the imageTag passed from context
                                                .ports(List.of(ContainerPort.builder()
                                                        .containerPort(8080) // Adjust if needed
                                                        .build()))
                                                .build()))
                                        .build())
                                .build())
                        .build())
                .build());

        // Optionally, define a Service to expose your application
        new KubeService(this, "my-service", KubeServiceProps.builder()
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

    public static void main(String[] args) {
        final App app = new App();

        // Fetch imageTag and appLabel from context
        String imageTag = (String) app.getNode().tryGetContext("image_tag");
        String appLabel = "my-app"; // Or fetch from context if needed

        new Main(app, "getting-started", appLabel, imageTag);
        app.synth();
    }
}
