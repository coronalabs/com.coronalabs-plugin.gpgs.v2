local gpgs = require('plugin.gpgs.v2')
local json = require('json')

local newButton = require('classes.button').newButton

local filename = 'snapshot1'
local snapshotId = ''

local group = display.newGroup()

newButton({
	g = group, index = 1,
	label = 'Load snapshots',
	onRelease = function()
		gpgs.snapshots.load({
			reload = true,
			listener = function(event)
				print('Load event:', json.prettify(event))
			end
		})
	end
})

newButton({
	g = group, index = 2,
	label = 'Get limits',
	onRelease = function()
		print(json.prettify(gpgs.snapshots.getLimits()))
	end
})

newButton({
	g = group, index = 3,
	label = 'Show',
	onRelease = function()
		gpgs.snapshots.show({
			title = 'Custom Title',
			listener = function(event)
				print('Show event:', json.prettify(event))
			end
		})
	end
})

newButton({
	g = group, index = 4,
	label = 'Open snapshot',
	onRelease = function()
		gpgs.snapshots.open({
			filename = filename,
			create = true,
			listener = function(event)
				print('Open event:', json.prettify(event))
				snapshotId = event.snapshotId
			end
		})
	end
})

newButton({
	g = group, index = 5,
	label = 'Discard snapshot',
	onRelease = function()
		gpgs.snapshots.discard(snapshotId)
	end
})

newButton({
	g = group, index = 6,
	label = 'Delete snapshot',
	onRelease = function()
		gpgs.snapshots.delete({
			snapshotId = snapshotId,
			listener = function(event)
				print('Delete event:', json.prettify(event))
			end
		})
	end
})

newButton({
	g = group, index = 7,
	label = 'Get snapshot content',
	onRelease = function()
		local snapshot = gpgs.snapshots.getSnapshot(snapshotId)
		if snapshot then
			print('Snapshot:', json.prettify(snapshot))
			print('Data:', snapshot.contents.read())
		end
	end
})

newButton({
	g = group, index = 8,
	label = 'Save snapshot',
	onRelease = function()
		local snapshot = gpgs.snapshots.getSnapshot(snapshotId)
		if snapshot then
			snapshot.contents.write('New data inside the snapshot')
			gpgs.snapshots.save({
				snapshotId = snapshot.id,
				description = 'Save Slot ' .. snapshot.id,
				playedTime = '12151',
				progress = 150,
				image = {filename = 'images/snapshot.png'},
				listener = function(event)
					print('Save event:', json.prettify(event))
				end
			})
		end
	end
})

return group
