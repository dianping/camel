var DataSourceTree = function(options) {
	this._data 	= options.data;
	this._delay = options.delay;
}

DataSourceTree.prototype.data = function(options, callback) {
	var self = this;
	var $data = null;

	if(!("name" in options) && !("type" in options)){
		$data = this._data;//the root tree
		callback({ data: $data });
		return;
	}
	else if("type" in options && options.type == "folder") {
		if("additionalParameters" in options && "children" in options.additionalParameters)
			$data = options.additionalParameters.children;
		else $data = {}//no data
	}
	
	if($data != null)//this setTimeout is only for mimicking some random delay
		setTimeout(function(){callback({ data: $data });} , parseInt(Math.random() * 500) + 200);

	//you can retrieve your data from a server using ajax call
};

var tree_data = {
	'for-sale' : {name: 'For Sale', type: 'folder'}	,
	'vehicles' : {name: 'Vehicles', type: 'folder'}	,
	'rentals' : {name: 'Rentals', type: 'folder'}	,
	'real-estate' : {name: 'Real Estate', type: 'folder'}	,
	'pets' : {name: 'Pets', type: 'folder'}	,
	'tickets' : {name: 'Tickets', type: 'item'}	,
	'services' : {name: 'Services', type: 'item'}	,
	'personals' : {name: 'Personals', type: 'item'}
}
tree_data['for-sale']['additionalParameters'] = {
	'children' : {
		'appliances' : {name: 'Appliances', type: 'item'},
		'arts-crafts' : {name: 'Arts & Crafts', type: 'item'},
		'clothing' : {name: 'Clothing', type: 'item'},
		'computers' : {name: 'Computers', type: 'item'},
		'jewelry' : {name: 'Jewelry', type: 'item'},
		'office-business' : {name: 'Office & Business', type: 'item'},
		'sports-fitness' : {name: 'Sports & Fitness', type: 'item'}
	}
}
tree_data['vehicles']['additionalParameters'] = {
	'children' : {
		'cars' : {name: 'Cars', type: 'folder'},
		'motorcycles' : {name: 'Motorcycles', type: 'item'},
		'boats' : {name: 'Boats', type: 'item'}
	}
}
tree_data['vehicles']['additionalParameters']['children']['cars']['additionalParameters'] = {
	'children' : {
		'classics' : {name: 'Classics', type: 'item'},
		'convertibles' : {name: 'Convertibles', type: 'item'},
		'coupes' : {name: 'Coupes', type: 'item'},
		'hatchbacks' : {name: 'Hatchbacks', type: 'item'},
		'hybrids' : {name: 'Hybrids', type: 'item'},
		'suvs' : {name: 'SUVs', type: 'item'},
		'sedans' : {name: 'Sedans', type: 'item'},
		'trucks' : {name: 'Trucks', type: 'item'}
	}
}

tree_data['rentals']['additionalParameters'] = {
	'children' : {
		'apartments-rentals' : {name: 'Apartments', type: 'item'},
		'office-space-rentals' : {name: 'Office Space', type: 'item'},
		'vacation-rentals' : {name: 'Vacation Rentals', type: 'item'}
	}
}
tree_data['real-estate']['additionalParameters'] = {
	'children' : {
		'apartments' : {name: 'Apartments', type: 'item'},
		'villas' : {name: 'Villas', type: 'item'},
		'plots' : {name: 'Plots', type: 'item'}
	}
}
tree_data['pets']['additionalParameters'] = {
	'children' : {
		'cats' : {name: 'Cats', type: 'item'},
		'dogs' : {name: 'Dogs', type: 'item'},
		'horses' : {name: 'Horses', type: 'item'},
		'reptiles' : {name: 'Reptiles', type: 'item'}
	}
}

var treeDataSource = new DataSourceTree({data: tree_data});











var tree_data_2 = {
	'pictures' : {name: 'Pictures', type: 'folder'}	,
	'music' : {name: 'Music', type: 'folder'}	,
	'video' : {name: 'Video', type: 'folder'}	,
	'documents' : {name: 'Documents', type: 'folder'}	,
	'backup' : {name: 'Backup', type: 'folder'}	,
	'readme' : {name: '<img src="'+$assets+'/icons/txt.png" /> ReadMe.txt', type: 'item'},
	'manual' : {name: '<img src="'+$assets+'/icons/html.png" /> Manual.html', type: 'item'}
}
tree_data_2['music']['additionalParameters'] = {
	'children' : [
		{name: '<img src="'+$assets+'/icons/ogg.png" /> song1.ogg', type: 'item'},
		{name: '<img src="'+$assets+'/icons/ogg.png" /> song2.ogg', type: 'item'},
		{name: '<img src="'+$assets+'/icons/ogg.png" /> song3.ogg', type: 'item'},
		{name: '<img src="'+$assets+'/icons/ogg.png" /> song4.ogg', type: 'item'},
		{name: '<img src="'+$assets+'/icons/ogg.png" /> song5.ogg', type: 'item'}
	]
}
tree_data_2['video']['additionalParameters'] = {
	'children' : [
		{name: '<img src="'+$assets+'/icons/divx.png" /> movie1.avi', type: 'item'},
		{name: '<img src="'+$assets+'/icons/divx.png" /> movie2.avi', type: 'item'},
		{name: '<img src="'+$assets+'/icons/divx.png" /> movie3.avi', type: 'item'},
		{name: '<img src="'+$assets+'/icons/divx.png" /> movie4.avi', type: 'item'},
		{name: '<img src="'+$assets+'/icons/divx.png" /> movie5.avi', type: 'item'}
	]
}
tree_data_2['pictures']['additionalParameters'] = {
	'children' : {
		'wallpapers' : {name: 'Wallpapers', type: 'folder'},
		'camera' : {name: 'Camera', type: 'folder'}
	}
}
tree_data_2['pictures']['additionalParameters']['children']['wallpapers']['additionalParameters'] = {
	'children' : [
		{name: '<img src="'+$assets+'/icons/jpg.png" /> wallpaper1.jpg', type: 'item'},
		{name: '<img src="'+$assets+'/icons/jpg.png" /> wallpaper2.jpg', type: 'item'},
		{name: '<img src="'+$assets+'/icons/jpg.png" /> wallpaper3.jpg', type: 'item'},
		{name: '<img src="'+$assets+'/icons/jpg.png" /> wallpaper4.jpg', type: 'item'}
	]
}
tree_data_2['pictures']['additionalParameters']['children']['camera']['additionalParameters'] = {
	'children' : [
		{name: '<img src="'+$assets+'/icons/jpg.png" /> photo1.jpg', type: 'item'},
		{name: '<img src="'+$assets+'/icons/jpg.png" /> photo2.jpg', type: 'item'},
		{name: '<img src="'+$assets+'/icons/jpg.png" /> photo3.jpg', type: 'item'},
		{name: '<img src="'+$assets+'/icons/jpg.png" /> photo4.jpg', type: 'item'},
		{name: '<img src="'+$assets+'/icons/jpg.png" /> photo5.jpg', type: 'item'},
		{name: '<img src="'+$assets+'/icons/jpg.png" /> photo6.jpg', type: 'item'}
	]
}


tree_data_2['documents']['additionalParameters'] = {
	'children' : [
		{name: '<img src="'+$assets+'/icons/pdf.png" /> document1.pdf', type: 'item'},
		{name: '<img src="'+$assets+'/icons/doc.png" /> document2.doc', type: 'item'},
		{name: '<img src="'+$assets+'/icons/doc.png" /> document3.doc', type: 'item'},
		{name: '<img src="'+$assets+'/icons/pdf.png" /> document4.pdf', type: 'item'},
		{name: '<img src="'+$assets+'/icons/doc.png" /> document5.doc', type: 'item'}
	]
}

tree_data_2['backup']['additionalParameters'] = {
	'children' : [
		{name: '<img src="'+$assets+'/icons/zip.png" /> backup1.zip', type: 'item'},
		{name: '<img src="'+$assets+'/icons/zip.png" /> backup2.zip', type: 'item'},
		{name: '<img src="'+$assets+'/icons/zip.png" /> backup3.zip', type: 'item'},
		{name: '<img src="'+$assets+'/icons/zip.png" /> backup4.zip', type: 'item'}
	]
}
var treeDataSource2 = new DataSourceTree({data: tree_data_2});