variable "aws_access_key" {
  description = "AWS access key"
}

variable "aws_secret_key" {
  description = "AWS secret key"
}

variable "aws_region" {
  description = "AWS region"
  default = "us-east-1"
}

variable "app_count" {
  description = "Number of application servers to deploy"
  default = 1
}

variable "instance_type" {
  description = "AWS instance type for nodes"
  default = "t2.medium"
}

# http://cloud-images.ubuntu.com/locator/ec2/
# Name = trusty, Instance Type = hvm:ebs-ssd
variable "amis" {
  description = "Ubuntu 14.04 AMIs by region"
  default = {
    ap-northeast-1 = "ami-b405bfb4"
    ap-southeast-1 = "ami-72353b20"
    ap-southeast-2 = "ami-3d054707"
    cn-north-1 = "ami-b0d34f89"
    eu-central-1 = "ami-8ae0e797"
    eu-west-1 = "ami-92401ce5"
    sa-east-1 = "ami-d1a129cc"
    us-east-1 = "ami-2dcf7b46"
    us-gov-west-1 = "ami-bbb9da98"
    us-west-1 = "ami-976d93d3"
    us-west-2 = "ami-97d5c0a7"
  }
}

variable "vpc_cidr" {
  description = "VPC CIDR block"
  default = "10.0.0.0/16"
}

variable "public_subnet_cidr" {
  description = "CIDR for public subnet"
  default = "10.0.0.0/24"
}

variable "private_subnet_cidr" {
  description = "CIDR for private subnet"
  default = "10.0.1.0/24"
}
