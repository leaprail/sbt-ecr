# sbt-ecr

An [SBT](http://www.scala-sbt.org/) plugin for managing [Docker](http://docker.io) images within [Amazon ECR](https://aws.amazon.com/ecr/).

> **Note**: This is a fork of the original [sbt-ecr](https://github.com/sjednac/sbt-ecr) project, now maintained by [Leaprail](https://github.com/leaprail). This fork has been migrated to use AWS SDK v2.0 and includes additional features and improvements.

## Features

* Create ECR repositories using `ecr:createRepository`
* Login to the remote registry using `ecr:login`
* Push local images using `ecr:push`
* Built-in support for AWS IAM Identity Center (SSO) authentication
* Uses AWS SDK v2 for all AWS interactions

## Installation

Add the following to your `project/plugins.sbt` file:

    addSbtPlugin("com.leaprail" % "sbt-ecr" % "1.0.0")

Add ECR settings to your `build.sbt`. The following snippet assumes a Docker image build using [sbt-native-packager](https://github.com/sbt/sbt-native-packager):

    import software.amazon.awssdk.regions.Region
    
    enablePlugins(EcrPlugin)

    region           in Ecr := Region.US_EAST_1
    repositoryName   in Ecr := (packageName in Docker).value
    localDockerImage in Ecr := (packageName in Docker).value + ":" + (version in Docker).value

    // Create the repository before authentication takes place (optional)
    login in Ecr := ((login in Ecr) dependsOn (createRepository in Ecr)).value

    // Authenticate and publish a local Docker image before pushing to ECR
    push in Ecr := ((push in Ecr) dependsOn (publishLocal in Docker, login in Ecr)).value
    
Keep in mind that `ecr:createRepository` is a completely optional step. If you have a [managed infrastructure](https://en.wikipedia.org/wiki/Infrastructure_as_code) (e.g. create everything, including the repository, using [AWS CloudFormation](https://aws.amazon.com/cloudformation/), [Terraform](https://www.terraform.io/) or some other tool), then it might be better to skip this step, and assume that the repository exist, when you trigger the process.

That being said, it's a convenient feature, when you don't rely on any tool like this. We support several policy-related settings, that will allow you to fine-tune the repository, if needed (read ahead).

## Usage

The plugin now supports the latest AWS authentication methods, following the [AWS SDK credential provider chain](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html):

* Provide `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` as [environment variables](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-environment.html).
* Use Java system properties: `aws.accessKeyId` and `aws.secretKey`.
* Use an AWS profile configured in the AWS credentials or config file.
* Use AWS IAM Identity Center (SSO) authentication.
* Use Web Identity Token credentials (for EKS/Kubernetes).
* Use the [EC2 instance profile](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/iam-roles-for-amazon-ec2.html) when running on EC2.
* Use ECS container credentials when running in ECS.

### Local Development with AWS Profiles

1. **Standard Profile**: Configure an AWS profile according to the [AWS CLI documentation](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-profiles.html):

   ```
   AWS_PROFILE=your_profile_name sbt ecr:push
   ```

2. **IAM Identity Center (SSO) Authentication**:
   
   First, configure an SSO profile in your AWS config file:

   ```
   [profile my-sso-profile]
   sso_start_url = https://my-sso-portal.awsapps.com/start
   sso_region = us-east-1
   sso_account_id = 123456789012
   sso_role_name = SSOReadOnlyRole
   region = us-west-2
   output = json
   ```

   Then login with the AWS CLI before running sbt:

   ```
   aws sso login --profile my-sso-profile
   AWS_PROFILE=my-sso-profile sbt ecr:push
   ```

## Tagging

By default, the produced image will be tagged as "latest". It is possible to provide arbitrary additional tags,
 for example to add the version tag to the image:
    
    repositoryTags in Ecr ++= Seq(version.value)
    
If you don't want latest tag on your image you could override the ```repositoryTags``` value completely:
 
    repositoryTags in Ecr := Seq(version.value)

If you want to make the tag environment-dependent you can use the following template:

    repositoryTags in Ecr := sys.env.get("VERSION_TAG").map(Seq(_)).getOrElse(Seq("latest"))

And trigger the process using:

    VERSION_TAG=myfeature sbt ecr:push

## Tag immutability

By default, when the `createRepository` task is executed, the new repository will have **Tag immutability**
disabled. You can control this behavior using the following setting:

    imageTagsMutable in Ecr := false
 
## Cross account publishing

By default, when the `login` task is executed, authentication will target the **registry id** and **repository domain** of the AWS account belonging to the role used.

If you need cross account authentication, you can override registry domain and target any registry id.

Example usage:

    repositoryDomain in Ecr := Some("myecr.example.com")
    registryIds in Ecr ++= Seq("your AWS account id")

## Repository security policy configuration

By default, when the `createRepository` task is executed, the new repository does not have a **security policy**
attached. 

When you set `repositoryPolicyText` in your `build.sbt` file, and the `createRepository` is called, the created
repository will have the configured policy. 

Example usage:
    
    repositoryPolicyText in Ecr := Some(IO.read(file("project") / "ecrpolicy.json")) 
    
Then in the `project/ecrpolicy.json` you can set your policy text. For example:
    
    {
      "Version": "2012-10-17",
      "Statement": [
        {
          "Sid": "BuildServerAccess",
          "Effect": "Allow",
          "Principal": {
            "AWS": [
              "arn:aws:iam::YOUR_ACCOUNT_ID_HERE:role/YOUR_IAM_ROLE_NAME_HERE"
            ]
          },
          "Action": [
            "ecr:*"
          ]
        }
      ]
    }
 
Configuring `repositoryPolicyText` will not affect existing repositories.

## Repository lifecycle policy configuration

Configuring the repository lifecycle policy works the same as configuring the policy in the previous chapter.

By default, when the `createRepository` task is executed, the new repository does not have a **lifecycle 
policy** attached. 

When you set `repositoryLifecyclePolicyText` in your `build.sbt` file, and the `createRepository` is called, the created
repository will have the configured lifecycle policy. 

Example usage:
    
    repositoryLifecyclePolicyText in Ecr := Some(IO.read(file("project") / "ecrlifecyclepolicy.json")) 
    
Then in the `project/ecrlifecyclepolicy.json` you can set your policy text. For example:
    
    {
      "rules": [
        {
          "rulePriority": 10,
          "description": "Lifecycle of release branch images",
          "selection": {
            "tagStatus": "tagged",
            "tagPrefixList": [
              "release"
            ],
            "countType": "imageCountMoreThan",
            "countNumber": 20
          },
          "action": {
            "type": "expire"
          }
        }
      ]
    }
 
Configuring `repositoryLifecyclePolicyText` will not affect existing repositories.


