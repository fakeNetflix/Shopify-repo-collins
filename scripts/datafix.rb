require 'json'
require 'chef'

AUTH="dale.hamel:collins"
HOST="util11.chi.shopify.com:9000"

Chef::Config.from_file("/home/dale.hamel/.chef/knife.rb")

input = `curl --basic -u #{AUTH} 'http://#{HOST}/api/assets?type=SERVER_NODE&size=300'`
input = JSON.parse(input)

nodes = Chef::Node.list.keys
VLANS = {
  "172.16.0" => "DMZ",
  "172.16.1" => "DB",
  "172.16.2" => "MANAGEMENT",
  "172.16.3" => "VLAN103",
  "172.16.4" => "NEXTGEN",
}

input["data"]["Data"].each do |thing|
  tag = thing["ASSET"]["TAG"]
  if name = thing["ATTRIBS"]["0"]["NAME"]
    next if name =~ /shopify\.com$/

    chefdata = nil
    if name =~ /vertica|importer/
      name = "#{name}.warehouse.chi.shopify.com"
    else
      name = "#{name}.chi.shopify.com"

    end

    puts name
    unless nodes.include? name
      puts "#{name} isn't a Chef-node"
    else 
      chefdata = Chef::Node.load(name).attributes

      systemdata = chefdata['dmi']['system']
      manufacturer = systemdata['manufacturer']
      product = systemdata['product_name']
      serial_number =  systemdata['serial_number']


      puts `curl -s --basic -u #{AUTH} --data-urlencode 'attribute=VENDOR;#{manufacturer}' http://#{HOST}/api/asset/#{tag}`
      puts `curl -s --basic -u #{AUTH} --data-urlencode 'attribute=PRODUCT;#{product}' http://#{HOST}/api/asset/#{tag}`
      puts `curl -s --basic -u #{AUTH} --data-urlencode 'attribute=SERIAL_NUMBER;#{serial_number}' http://#{HOST}/api/asset/#{tag}`


      gateway = chefdata["network"]["default_gateway"]

      chefdata["network"]["interfaces"].each do | iface, iface_data |
        if iface_data.has_key?("addresses")
          addresses = iface_data["addresses"]
          addresses.each do | addr, addr_data |
            if addr_data["family"] == "inet"
              ip = addr
              mask = addr_data["netmask"]
              gw = gateway
              if gw.split(".")[0..2].join(".") == ip.split(".")[0..2].join(".")

                vlan = gw.split(".")[0..2].join(".") 
                pool = VLANS[vlan]

                addrs = []
                thing['ADDRESSES'].each do | addr |
                  addrs.push(addr['ADDRESS'])
                end
                if not addrs.include?(ip)
                  puts `curl  -s --basic -u #{AUTH} -X POST -d address=#{ip} -d gateway=#{gw} -d netmask=#{mask} -d pool=#{pool} http://#{HOST}/api/asset/#{tag}/address`
                  puts `curl -s --basic -u #{AUTH} -X DELETE http://#{HOST}/api/asset/#{tag}/attribute/IP`
                end
              end
            end

          end
        end
      end 
    end

    puts `curl -s --basic -u #{AUTH} --data-urlencode 'attribute=NAME;#{name}' http://#{HOST}/api/asset/#{tag}`
  else
    next if thing["ATTRIBS"]["0"]["NOTES"] =~ /Chassis/
    puts "Missing name on a thing."
    puts JSON.pretty_generate(thing)
  end

end
