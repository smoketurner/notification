resource "aws_instance" "app" {
  count = "${var.app_count}"
  ami = "${lookup(var.amis, var.aws_region)}"
  instance_type = "${var.instance_type}"
  depends_on = ["aws_instance.nat"]
  subnet_id = "${aws_subnet.private.id}"
  security_groups = [
    "${aws_security_group.default.id}"
  ]
  key_name = "${aws_key_pair.deployer.key_name}"
  source_dest_check = true
  user_data = "${file(\"cloud-config/app.yml\")}"
  root_block_device {
    volume_type = "gp2"
    volume_size = "20"
  }
  connection {
    user = "ubuntu"
    key_file = "ssh/insecure-deployer"
    bastion_host = "${aws_instance.nat.public_ip}"
  }
  provisioner "remote-exec" {
    inline = [
      "echo 'export DW_WORKER_ID=${count.index+1}' | sudo tee /etc/default/notification-application",
      "echo 'export DW_DATACENTER_ID=1' | sudo tee -a /etc/default/notification-application"
    ]
  }
  tags {
    Name = "app-${format("%02d", count.index+1)}"
    sshUser = "ubuntu"
    role = "app"
    DW_WORKER_ID = "${count.index+1}"
    DW_DATACENTER_ID = "1"
  }
}

resource "aws_elb" "app" {
  name = "notification-elb"
  subnets = ["${aws_subnet.public.id}"]
  security_groups = [
    "${aws_security_group.default.id}",
    "${aws_security_group.web.id}"
  ]

  listener {
    instance_port = 8080
    instance_protocol = "http"
    lb_port = 80
    lb_protocol = "http"
  }

  health_check {
    healthy_threshold = 2
    unhealthy_threshold = 2
    timeout = 3
    target = "HTTP:8180/healthcheck"
    interval = 5
  }

  instances = ["${aws_instance.app.*.id}"]
  cross_zone_load_balancing = true
  connection_draining = true

  tags {
    Name = "notification-elb"
  }
}
