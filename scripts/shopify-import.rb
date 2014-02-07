
require 'CSV'


$USER="blake"
$PASS="admin:first"
#todo= ["HR106","HS106","IL106","IM106","IN106","IO106","FO143","FN143","FM143"]
todo = []

racks = CSV.read("racks.csv", :headers=>true)
$info = CSV.read("info.csv", :headers=>true)


def findChassis(chassis)

    available = $info["Chassis"]
    found =  available.include?(chassis)

    attrs = {}
    if found
        index = available.index(chassis)
        $info.headers.each do | header |
            cell =  $info[header][index]
            if !cell.nil?
                data = cell.strip
                if !data.empty?
                    attrs[header] = data
                end
            end
        end

    end
    return attrs

end

def findTag(tag)

    available = $info["Tag"]
    found =  available.include?(tag)

    attrs = {}
    if found
        index = available.index(tag)
        $info.headers.each do | header |
            cell =  $info[header][index]
            if !cell.nil?
                data = cell.strip
                if !data.empty?
                    attrs[header] = data
                end
            end
        end

    end
    return attrs

end


def addAttrs(asset, attrs)
    attrs.each do | key, value |
        if key != "Chassis" and key != "Tag"
           key = key.sub(" ","_")
           puts `curl -s --basic -u "#{$USER}:#{$PASS}"  --data-urlencode 'attribute=#{key};#{value}'  http://localhost:9000/api/asset/#{asset}`
        end
    end

end

puts "chicago"
`curl -s --basic -u "#{$USER}:#{$PASS}" -X PUT --data-urlencode "type=DATA_CENTER" http://localhost:9000/api/asset/Chicago`


racks.headers.each do | header |
 
    if header !=  "RU" and todo.include?(header)
        rack="CHI-"+header
        racklabel = header
        row="CHI-"+header[0]+header[2..-1]
        rowlabel = header[0]
        room="CHI-"+header[2..-1]
        roomlabel=header[2..-1]

        # Make physical parents
        puts "Room #{room}"
        puts `curl -s --basic -u #{$USER}:#{$PASS} -X PUT --data-urlencode "type=DATA_ROOM" http://localhost:9000/api/asset/#{room}`
        puts `curl -s --basic -u #{$USER}:#{$PASS}  -d child=#{room}  -d label=#{roomlabel}  http://localhost:9000/api/hierarchy/Chicago`

        puts "Row #{row}"
        puts `curl -s --basic -u #{$USER}:#{$PASS} -X PUT --data-urlencode "type=DATA_ROW" http://localhost:9000/api/asset/#{row}`
        puts `curl -s --basic -u #{$USER}:#{$PASS}  -d child=#{row}  -d label=#{rowlabel} http://localhost:9000/api/hierarchy/#{room}`

        puts "Rack #{rack}"
        puts `curl -s --basic -u #{$USER}:#{$PASS} -X PUT --data-urlencode "type=RACK" http://localhost:9000/api/asset/#{rack}`
        puts `curl -s --basic -u #{$USER}:#{$PASS}  -d child=#{rack}  -d label=#{racklabel}  http://localhost:9000/api/hierarchy/#{row}`
        puts `curl -s --basic -u #{$USER}:#{$PASS}  --data-urlencode "attribute=RU_COUNT;#{racks["RU"].length}"  http://localhost:9000/api/asset/#{rack}`
        

        last_chassis = nil
        last_ru = nil

        0.upto(racks[header].length)  do | n |
            ru=racks["RU"][n]
            #puts "#{header}:#{ru}"

            chassis=racks[header][n]

            if last_chassis.nil?
                last_chassis = chassis
                last_ru = ru.to_i
            end

            if chassis != last_chassis
                temp = chassis
                chassis = last_chassis
                last_chassis = temp

                if chassis != "Empty"
                    attrs = findChassis(chassis)
                    start = ru.to_i + 1
                    chas_end = last_ru - 1
                    supported = true
                    chassis_label = ""

                    #create the asset
                    if chassis[0..2] == "CHI" and chassis.index("MX80") == nil # Server Chassis 
                        chassis_label = chassis.split("-")[2]
                        puts "Server #{chassis}, #{start}-#{chas_end}"
                        #puts attrs
                        puts `curl -s --basic -u "#{$USER}:#{$PASS}" -X PUT --data-urlencode "type=SERVER_CHASSIS" http://localhost:9000/api/asset/#{chassis}`
                    elsif chassis[0..1] == "pp"  # patch panel
                        puts "Patch #{chassis}, #{start}-#{chas_end}"
                        #puts attrs
                        puts `curl -s --basic -u "#{$USER}:#{$PASS}" -X PUT --data-urlencode "type=PATCH_PANEL" http://localhost:9000/api/asset/#{chassis}`

                    elsif chassis[0..2] == "PDU"  # pdu
                        puts "PDU #{chassis}, #{start}-#{chas_end}"
                        #puts attrs
                        puts `curl -s --basic -u "#{$USER}:#{$PASS}" -X PUT --data-urlencode "type=PDU" http://localhost:9000/api/asset/#{chassis}`
                    elsif chassis[0..1] == "sw" or chassis[0..4] == "newsw" #  switches
                        puts "Switch #{chassis}, #{start}-#{chas_end}"
                        #puts attrs
                        puts `curl -s --basic -u "#{$USER}:#{$PASS}" -X PUT --data-urlencode "type=SWITCH" http://localhost:9000/api/asset/#{chassis}`
                    elsif chassis[0..1] == "FW"  # firewalls 
                        puts "Firewall #{chassis}, #{start}-#{chas_end}"
                        #puts attrs
                        puts `curl -s --basic -u "#{$USER}:#{$PASS}" -X PUT --data-urlencode "type=FIREWALL" http://localhost:9000/api/asset/#{chassis}`
                    elsif chassis[0..1] == "F5"  # load balancer 
                        puts "Load #{chassis}, #{start}-#{chas_end}"
                        #puts attrs
                        puts `curl -s --basic -u "#{$USER}:#{$PASS}" -X PUT --data-urlencode "type=LOAD_BALANCER" http://localhost:9000/api/asset/#{chassis}`
                    elsif chassis[0..4] == "arbor"  # ddos 
                        puts "DDOS #{chassis}, #{start}-#{chas_end}"
                        #puts attrs
                        puts `curl -s --basic -u "#{$USER}:#{$PASS}" -X PUT --data-urlencode "type=DDOS_MITIGATE" http://localhost:9000/api/asset/#{chassis}`
                    elsif chassis[0..1] == "AC"  #  serial console
                        puts "Serial #{chassis}, #{start}-#{chas_end}"
                        #puts attrs
                        puts `curl -s --basic -u "#{$USER}:#{$PASS}" -X PUT --data-urlencode "type=SERIAL_SERVER" http://localhost:9000/api/asset/#{chassis}`
                    elsif !chassis.index("MX80").nil?  #  serial console
                        puts "Router #{chassis}, #{start}-#{chas_end}"
                        #puts attrs
                        puts `curl -s --basic -u "#{$USER}:#{$PASS}" -X PUT --data-urlencode "type=ROUTER" http://localhost:9000/api/asset/#{chassis}`

                    else
                        puts "NOT SUPPORTED #{chassis}, #{start}-#{chas_end}"
                        supported = false

                    end


                    if supported
                    
                        #do parenting
                        if chassis_label.empty?
                            chassis_label = chassis
                        end
                        puts `curl -s --basic -u "#{$USER}:#{$PASS}"  -d child=#{chassis}  -d label=#{chassis_label} -d start=#{start} -d end=#{chas_end} http://localhost:9000/api/hierarchy/#{rack}`
                        #add attributes
                        addAttrs(chassis, attrs)
                    end
                end
                last_ru = ru.to_i + 1
            end


        end
    end
end

$info['Tag'].each do | tag |

    if !tag.nil? and !tag.empty? and tag.split("-").length == 4 and tag[0..2] == "CHI" and tag.index("MX80") == nil # Server Node

        puts "Server Node #{tag}"
        attrs = findTag(tag)
        chassis = attrs["Chassis"]
        puts attrs

        # Create it
        puts `curl -s --basic -u "#{$USER}:#{$PASS}" -X PUT --data-urlencode "type=SERVER_NODE" http://localhost:9000/api/asset/#{tag}`

#        # Add chassis
#        puts `curl -s --basic -u "#{$USER}:#{$PASS}"  --data-urlencode 'CHASSIS_TAG=#{chassis}'   http://localhost:9000/api/asset/#{tag}`

        # parenting
        puts `curl -s --basic -u "#{$USER}:#{$PASS}"  -d child=#{tag}  -d label=#{tag} -d start=-1 -d end=-1 http://localhost:9000/api/hierarchy/#{chassis}`
        # attributes
        addAttrs(tag, attrs)

    end


end

