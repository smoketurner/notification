Notification Terraform
======================
This directory contains a set of [Terraform](https://terraform.io) configuration files that allow standing up a secure implementation of the Notification service on AWS. The majority of the best practices came from [Guide to automating a multi-tiered application securely on AWS with Docker and Terraform](https://www.airpair.com/aws/posts/ntiered-aws-docker-terraform-guide).

Getting Started
---------------
1. [Download Terraform](https://terraform.io/downloads.html)
2. From within the `terraform` directory, create a file called `terraform.tfvars` with your AWS credentials:
```
aws_access_key = "YOUR AWS ACCESS KEY"
aws_secret_key = "YOUR AWS SECRET KEY"
```
3. Generate a new SSH key by running:
```bash
ssh-keygen -t rsa -C "insecure-deployer" -P '' -f ssh/insecure-deployer
```
4. Run `terraform plan` to see all of the various AWS resources that will be created.
5. Run `terraform apply` to create the AWS resources (*NOTE* your AWS account will be charged accordingly for any resources that are created)
6. Run `./bin/ovpn-init` to initialize the [OpenVPN](https://openvpn.net/) server on the [bastion host](https://en.wikipedia.org/wiki/Bastion_host)
7. Run `./bin/ovpn-start` to start up the OpenVPN server on the bastion host
8. Run `./bin/ovpn-new-client $USER` to generate a new client certificate for your local user account
9. Run `./bin/ovpn-client-config $USER` to download the OpenVPN client configuration from the bastion host

You can then load in the OpenVPN client configuration in a product like [Viscosity](http://www.sparklabs.com/viscosity/) to VPN into the bastion host and directly access the servers on the private subnet.

```bash
ssh -t -i ssh/insecure-deployer ubuntu@(terraform output app.0.ip)
```

When you are finished, you can tear down all of the infrastructure by executing `terraform destroy`.

Architecture
------------
[!(aws_architecture.png)]
