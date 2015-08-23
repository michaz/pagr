<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
	<title>Leaflet GeoJSON Example</title>
	<meta charset="utf-8" />
	<script src="http://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.3/leaflet.js"></script>
	<link rel="stylesheet" href="http://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.3/leaflet.css" />
	<style>
		body {
			padding: 0;
			margin: 0;
		}
		html, body, #map {
			height: 100%;
			width: 100%;
		}
	</style>
</head>
<body>
	<div id="map"/>
	<script>
		var colors = new Map();
		function getRandomColor(feature)
        {
            var letters = '0123456789ABCDEF'.split('');
            var color = '#';
            for (var i = 0; i < 6; i++ )
            {
               color += letters[Math.round(Math.random() * 15)];
            }
            colors.set(feature.properties.cellid, color);
        	return color;
        }

        function getColorForCellUpdate(feature)
		{
			if(colors.has(feature.properties.ci)) {
				return colors.get(feature.properties.ci);
			} else {
				return 0;
			}
		}

		function getRadius(feature) {
			if(feature.properties.range && !isNaN(feature.properties.range)) {
				return feature.properties.range;
			} else {
				return 10;
			}
		}

		var map = L.map('map', {
        	center: [51.505, -0.09],
        	zoom: 13
        });

		L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token=pk.eyJ1IjoibWFwYm94IiwiYSI6IjZjNmRjNzk3ZmE2MTcwOTEwMGY0MzU3YjUzOWFmNWZhIn0.Y8bhBaUMqFiPrDRW9hieoQ', {
			maxZoom: 18,
			attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, ' +
				'<a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, ' +
				'Imagery © <a href="http://mapbox.com">Mapbox</a>',
			id: 'mapbox.light'
		}).addTo(map);

		var layer1 = L.geoJson(<c:import url="/routes?route=${param.route}" />)
    	layer1.addTo(map);

    	var layer2 = L.geoJson(<c:import url="/routes?route=${param.route}&cellTowers=true" />, {
        	pointToLayer: function (feature, latlng) {
            	return L.circle(latlng, getRadius(feature), {
            		color: getRandomColor(feature),
            	});
        	}
        })
        layer2.addTo(map);

		var layer3 = L.geoJson(<c:import url="/routes?route=${param.route}&cellUpdates=true" />, {
			pointToLayer: function (feature, latlng) {
				return L.circleMarker(latlng, {
				    radius: 8,
					color: getColorForCellUpdate(feature),
					weight: 1,
					opacity: 1,
					fillOpacity: 0.8
				});
			}
		})
		layer3.addTo(map);


    	map.fitBounds(layer1.getBounds());
	</script>
</body>
</html>
