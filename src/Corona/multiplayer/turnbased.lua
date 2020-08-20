local turnbased = {}

local json = require('json')
local gpgs = require('plugin.gpgs.v2')
local newButton = require('classes.button').newButton

local _W, _H = display.actualContentWidth, display.actualContentHeight
local _CX, _CY = display.contentCenterX, display.contentCenterY

function turnbased:newParticipant(params)
	local participant = display.newCircle(params.g, params.x, params.y, 30)
	participant:setFillColor(unpack(params.color))
	participant.id = params.id
	participant.color = params.color

	function participant:update(updateParams)
		self.x, self.y = updateParams.x, updateParams.y
	end

	function participant:touch(event)
		if event.phase == 'began' then
			self.xStart, self.yStart = self.x, self.y
			display.getCurrentStage():setFocus(self)
			self.isFocused = true
		elseif self.isFocused then
			if event.phase == 'moved' then
				self.x, self.y = self.xStart + event.x - event.xStart, self.yStart + event.y - event.yStart
			else
				display.getCurrentStage():setFocus(nil)
				self.isFocused = false
			end
		end
		return true
	end
	if params.isPlayer then
		participant:addEventListener('touch')
	end

	return participant
end

function turnbased:restore()
	if self.matchId then
		local match = gpgs.multiplayer.turnbased.getMatch(self.matchId)
		local playerParticipantId = match.getParticipantId(self.playerId)
		local playerPresent = false
		if match.getPayload() then
			local participantsData = json.decode(match.getPayload())
			for i = 1, #participantsData do
				local participantData = participantsData[i]
				participantData.isPlayer = participantData.id == playerParticipantId
				if participantData.isPlayer then
					playerPresent = true
				end
				if not self.participants[participantData.id] then
					participantData.g = self.group
					self.participants[participantData.id] = self:newParticipant(participantData)
				else
					self.participants[participantData.id]:update(participantData)
				end
			end
		end
		if not playerPresent then
			self.participants[playerParticipantId] = self:newParticipant({
				g = self.group,
				id = playerParticipantId,
				x = _CX, y = _CY,
				color = {math.random(), math.random(), math.random()},
				isPlayer = true
			})
		end
	end
end

function turnbased:show()
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

	gpgs.multiplayer.turnbased.setListener(function(event)
		print('Match event:', json.prettify(event))
	end)

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
                    gpgs.multiplayer.turnbased.join({
                        invitationId = event.invitation.id,
                        listener = function(event)
                            self.matchId = event.matchId
							self:startGameplay()
                        end
                    })
				elseif event.matchId then
					self.matchId = event.matchId
					self:startGameplay()
				end
			end)
		end
	})

    newButton({
		g = self.group, index = 2,
		label = 'Show matches',
		onRelease = function()
			gpgs.multiplayer.turnbased.showMatches(function(event)
				print('Show matches event:', json.prettify(event))
				if event.matchId then
					self.matchId = event.matchId
					self:startGameplay()
				end
			end)
		end
	})

	newButton({
		g = self.group, index = 3,
		label = 'Select players',
		onRelease = function()
			gpgs.multiplayer.turnbased.showSelectPlayers({
				minPlayers = 1,
				maxPlayers = 1,
				listener = function(event)
					print('Select players event:', json.prettify(event))
					if event.playerIds then
						gpgs.multiplayer.turnbased.create({
							playerIds = event.playerIds,
							listener = function(event)
								if event.matchId then
									self.matchId = event.matchId
									self:startGameplay()
								end
							end
						})
					end
				end
			})
		end
	})

	newButton({
		g = self.group, index = 4,
		label = 'Get match',
		onRelease = function()
			if self.matchId then
				local match = gpgs.multiplayer.turnbased.getMatch(self.matchId)
				print('Match:', json.prettify(match))
				print('canRematch', match.canRematch())
				print('getAvailableAutoMatchSlots', match.getAvailableAutoMatchSlots())
				print('getPayload', match.getPayload())
				print('getMainOpponentParticipantId', match.getMainOpponentParticipantId())
				print('getLastUpdatedTimestamp', match.getLastUpdatedTimestamp())
				print('getLastUpdaterParticipantId', match.getLastUpdaterParticipantId())
				print('getPreviousPayload', match.getPreviousPayload())
				print('getRematchId', match.getRematchId())
				print('getStatus', match.getStatus())
				print('getTurnStatus', match.getTurnStatus())
				print('getVersion', match.getVersion())
				print('isLocallyModified', match.isLocallyModified())
				local participantIds = match.getParticipantIds()
				for i = 1, #participantIds do
					print('Participant ' .. i .. ':', json.prettify(match.getParticipant(participantIds[i])))
					print('Participant ' .. i .. ' status:', match.getParticipantStatus(participantIds[i]))
				end
			end
		end
	})

	newButton({
		g = self.group, index = 5,
		label = 'Send',
		onRelease = function()
			if self.matchId then
				local match = gpgs.multiplayer.turnbased.getMatch(self.matchId)
				local matchData = {}
				local pendingParticipantId = match.getMainOpponentParticipantId()
				for id, p in pairs(self.participants) do
					local participant = {
						id = id,
						x = p.x,
						y = p.y,
						color = p.color
					}
					table.insert(matchData, participant)
				end
				print('matchId', self.matchId)
				print('payload', json.encode(matchData))
				print('pendingParticipantId', pendingParticipantId)
				gpgs.multiplayer.turnbased.send({
					matchId = self.matchId,
					payload = json.encode(matchData),
					pendingParticipantId = pendingParticipantId,
					listener = function(event)
						print(json.prettify(event))
					end
				})
			end
		end
	})

	newButton({
		g = self.group, index = 6,
		label = 'Finish',
		onRelease = function()
			if self.matchId then
				local matchData = {}
				for id, p in pairs(self.participants) do
					local participant = {
						id = id,
						x = p.x,
						y = p.y,
						color = p.color
					}
					table.insert(matchData, participant)
				end
				gpgs.multiplayer.turnbased.finish({
					matchId = self.matchId,
					payload = json.encode(matchData),
					listener = function(event)
						print(json.prettify(event))
					end
				})
			end
		end
	})

	newButton({
		g = self.group, index = 7,
		label = 'Leave',
		onRelease = function()
			self:hide()
		end
	})
end

function turnbased:startGameplay()
	self:restore()
end

function turnbased:hide()
	self.group:removeSelf()
	self.participants = nil
	self.matchId = nil
	gpgs.multiplayer.invitations.removeListener()
	gpgs.multiplayer.turnbased.removeListener()
end

return turnbased
