<%@ page contentType="text/html; charset=UTF-8" %>
<html>
<head>
	<title>Leaflet GeoJSON Example</title>
	<meta charset="utf-8" />

	<meta name="viewport" content="width=device-width, initial-scale=1.0">

	<script src='https://api.tiles.mapbox.com/mapbox.js/v2.2.1/mapbox.js'></script>
	<link href='https://api.tiles.mapbox.com/mapbox.js/v2.2.1/mapbox.css' rel='stylesheet' />
</head>
<body>
	<div id="map" style="width: 600px; height: 400px"></div>


	<script>
		var map = L.map('map');

		L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token=pk.eyJ1IjoibWFwYm94IiwiYSI6IjZjNmRjNzk3ZmE2MTcwOTEwMGY0MzU3YjUzOWFmNWZhIn0.Y8bhBaUMqFiPrDRW9hieoQ', {
			maxZoom: 18,
			attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, ' +
				'<a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, ' +
				'Imagery © <a href="http://mapbox.com">Mapbox</a>',
			id: 'mapbox.light'
		}).addTo(map);

		var layer1 = L.mapbox.featureLayer()
		    .on('ready', run)
    		.loadURL('/routes?route=${param.route}');
    	layer1.addTo(map);

    	var layer2 = L.mapbox.featureLayer()
        	.on('ready', run)
            .loadURL('/routes?route=${param.route}&cellTowers=true');
        layer2.addTo(map);

    	function run() {
    		layer.eachLayer(function(l) {
    			map.fitBounds(layer.getBounds());
    		});
    	}
	</script>
</body>
</html>
