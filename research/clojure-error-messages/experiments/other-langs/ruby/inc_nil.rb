config = { port: 8080 }

def next_port(c)
  c[:prot] + 1
end

next_port(config)
