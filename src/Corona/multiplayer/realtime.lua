local realtime = {}

local json = require('json')
local gpgs = require('plugin.gpgs.v2')
local newButton = require('classes.button').newButton

local _W, _H = display.actualContentWidth, display.actualContentHeight
local _CX, _CY = display.contentCenterX, display.contentCenterY

function realtime:newPlayer(params)
	local player = display.newCircle(params.g, params.x, params.y, 30)
	player.color = {0.5 + math.random() / 2, 0.5 + math.random() / 2, 0.5 + math.random() / 2}
	player:setFillColor(unpack(player.color))

	local super = self
	function player:send()
		gpgs.multiplayer.realtime.sendUnreliably({
			roomId = super.roomId,
			payload = json.encode({id = super.playerId, color = self.color, x = self.x, y = self.y})
		})
	end
	player:send()

	function player:touch(event)
		if event.phase == 'began' then
			self.xStart, self.yStart = self.x, self.y
			display.getCurrentStage():setFocus(self)
			self.isFocused = true
		elseif self.isFocused then
			if event.phase == 'moved' then
				self.x, self.y = self.xStart + event.x - event.xStart, self.yStart + event.y - event.yStart
				self:send()
			else
				display.getCurrentStage():setFocus(nil)
				self.isFocused = false
			end
		end
	end
	player:addEventListener('touch')

	return player
end

function realtime:newParticipant(params)
	local participant = display.newCircle(params.g, params.x, params.y, 30)
	participant:setFillColor(unpack(params.color))
	participant.id = params.id

	function participant:update(updateParams)
		self.x, self.y = updateParams.x, updateParams.y
	end

	return participant
end

function realtime:show()
	self.playerId = ''
	self.participants = {}
	display.remove(self.group)
	self.group = display.newGroup()
	local background = display.newRect(self.group, _CX, _CY, _W, _H)
	background:setFillColor(0, 0.2, 0.4)
	print(json.prettify(gpgs.multiplayer.getLimits() or '{}'))

	gpgs.multiplayer.invitations.setListener(function(event)
		print('Invitations event:', json.prettify(event))
	end)

	gpgs.multiplayer.realtime.setListeners({
		message = function(event)
			print('Message event:', json.prettify(event))
			local participantData = json.decode(event.payload)
			if not self.participants[participantData.id] then
				participantData.g = self.group
				self.participants[participantData.id] = self:newParticipant(participantData)
			else
				self.participants[participantData.id]:update(participantData)
			end
		end,
		peer = function(event)
			print('Peer event:', json.prettify(event))
		end,
		room = function(event)
			print('Room event:', json.prettify(event))
			if event.phase == 'created' then
				self.roomId = event.roomId
				gpgs.multiplayer.realtime.showWaitingRoom({
					roomId = event.roomId
				})
			elseif event.phase == 'connected' then
				self.roomId = event.roomId
				self:startGameplay()
			elseif event.phase == 'left' then
				self:hide()
			end
		end
	})

	gpgs.players.load({
		listener = function(event)
			if not event.isError then
				self.playerId = event.players[1].id
			end
		end
	})

	newButton({
		g = self.group, index = 1,
		label = 'Show invitations',
		onRelease = function()
			gpgs.multiplayer.invitations.show(function(event)
				print('Show invitations event:', json.prettify(event))
				if event.invitation then
					gpgs.multiplayer.realtime.join(event.invitation.id)
				end
			end)
		end
	})

	newButton({
		g = self.group, index = 2,
		label = 'Select players',
		onRelease = function()
			gpgs.multiplayer.realtime.showSelectPlayers({
				listener = function(event)
					print('Select players event:', json.prettify(event))
					if event.playerIds then
						gpgs.multiplayer.realtime.create({
							playerIds = event.playerIds
						})
					end
				end
			})
		end
	})

	newButton({
		g = self.group, index = 3,
		label = 'Get room',
		onRelease = function()
			if self.roomId then
				local room = gpgs.multiplayer.realtime.getRoom(self.roomId)
				print('Room:', json.prettify(room))
				print('Room AutoMatchWaitEstimateSeconds:', room.getAutoMatchWaitEstimateSeconds())
				local participantIds = room.getParticipantIds()
				for i = 1, #participantIds do
					print('Participant ' .. i .. ':', json.prettify(room.getParticipant(participantIds[i])))
					print('Participant ' .. i .. ' status:', room.getParticipantStatus(participantIds[i]))
				end
			end
		end
	})

	newButton({
		g = self.group, index = 4,
		label = 'Leave',
		onRelease = function()
			if self.roomId then
				gpgs.multiplayer.realtime.leave(self.roomId)
			end
		end
	})
end

function realtime:startGameplay()
	self.player =self:newPlayer({g = self.group, x = _CX, y = _CY})
end

function realtime:hide()
	self.group:removeSelf()
	self.player = nil
	self.roomId = nil
	self.participants = nil
	gpgs.multiplayer.invitations.removeListener()
	gpgs.multiplayer.realtime.removeListeners()
end

return realtime
