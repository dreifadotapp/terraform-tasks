variable "content" {
  type = string
  default = "foo!"
}

resource "local_file" "foo" {
    content     = var.content
    filename = "${path.module}/foo.bar"
}

output "updated_content" {
  value = local_file.foo.content
}