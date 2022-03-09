variable "content" {
  type = string
  default = "foo!"
}

resource "local_file" "foo" {
    content     = var.content
    filename = "${path.module}/foo.bar"
}