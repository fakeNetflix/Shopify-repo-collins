
var s;
function init() {


  var nodes = [];
  var children = {};
  var parents = {};
  var links = {};
  var clusters = {};
  var url_endpoint = "/api/hierarchy/nodes"
  $.ajax({
      url: url_endpoint,
      type: 'GET',
      async: false,
      success: function(result) {

          result['data']['values'].forEach( function( linkinfo ){
              asset = linkinfo['ASSET']
              child = linkinfo['CHILD_TAG']

              if (nodes.indexOf(asset) <0){nodes.push(asset)}
              if (nodes.indexOf(child) <0){nodes.push(child)}

              children[child] = linkinfo
              parents[child] = asset

              if (links[asset])
              {
                 links[asset].push(linkinfo)
              }
              else
              {
                 links[asset] = [linkinfo] 
              }
          })
      }
  });

  $.each( children, function( key, value ) {
      clusters[key] = {'id':key, 'nodes':[], 'color':"#ff0000"}
  });

  graph = {nodes:[],edges:[]}
  nodes.forEach( function( asset ) {

      if (children[asset])
      {
          label = children[asset]['CHILD_LABEL']
          graph.nodes.push(  { id:asset, size:15, label:label, color:"#ff0000", 'x':Math.random(), 'y':Math.random(), 'cluster':clusters[parents[asset]]})
      }
      else
      {
          label = asset;
          graph.nodes.push( { id:asset, size:15, label:label, color:"#ff0000", 'x':Math.random(), 'y':Math.random(), 'cluster':clusters[asset]})
      } 



  });


  nodes.forEach( function( asset ) {
      if(links[asset]){
          links[asset].forEach( function( link) {
              child = link['CHILD_TAG']
              graph.edges.push( { id:asset+"_"+child, source:asset, target:child} ); 
          });
      }

  });
  //sigInst.startForceAtlas2();
  //
  s = new sigma({
      graph:graph, 
      container:'sitemap', 
      settings:{
          defaultLabelColor:"#ff0000",
          defaultNodeColor:"#ff0000",
          minNodeSize: 0.5,
          maxNodeSize: 5,
          minEdgeSize:1,
          maxEdgeSize:1
      }
  })
  s.bind('clickNode',function(event){
      node = event.data.node
      window.location = "/asset/"+node.id
  });

  window.setTimeout(function() {
      s.stopForceAtlas2();
   }, 5000);

  s.startForceAtlas2();

}

$("#atlas-stop").click(function(){
      s.stopForceAtlas2();
});

$("#atlas-start").click(function(){
      s.startForceAtlas2();

      window.setTimeout(function() {
          s.stopForceAtlas2();
       }, 5000);

});


if (document.addEventListener) {
      document.addEventListener('DOMContentLoaded', init, false);
} else {
      window.onload = init;
}
