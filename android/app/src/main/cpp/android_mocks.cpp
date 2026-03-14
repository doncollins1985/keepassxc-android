#include "core/EntryAttachments.h"
#include "core/CustomData.h"
#include "core/FileWatcher.h"
#include "keys/drivers/YubiKey.h"
#include "core/Database.h"
#include "core/ModifiableObject.h"
#include "core/Config.h"
#include <QXmlStreamReader>
#include <QTimer>
#include <QCoreApplication>

// EntryAttachments signals
void EntryAttachments::aboutToBeAdded(const QString&) {}
void EntryAttachments::added(const QString&) {}
void EntryAttachments::aboutToBeRemoved(const QString&) {}
void EntryAttachments::removed(const QString&) {}
void EntryAttachments::keyModified(const QString&) {}
void EntryAttachments::valueModifiedExternally(const QString&, const QString&) {}
void EntryAttachments::aboutToBeReset() {}
void EntryAttachments::reset() {}

// CustomData signals
void CustomData::aboutToBeAdded(const QString&) {}
void CustomData::added(const QString&) {}
void CustomData::aboutToBeRemoved(const QString&) {}
void CustomData::removed(const QString&) {}
void CustomData::aboutToRename(const QString&, const QString&) {}
void CustomData::renamed(const QString&, const QString&) {}
void CustomData::aboutToBeReset() {}
void CustomData::reset() {}

// FileWatcher (mock implementation)
FileWatcher::FileWatcher(QObject* parent) : QObject(parent) {}
FileWatcher::~FileWatcher() {}
void FileWatcher::start(const QString&, int, int) {}
void FileWatcher::stop() {}
void FileWatcher::fileChanged(const QString&) {}
bool FileWatcher::hasSameFileChecksum() { return false; }
void FileWatcher::pause() {}
void FileWatcher::resume() {}
void FileWatcher::checkFileChanged() {}
QByteArray FileWatcher::calculateChecksum() { return QByteArray(); }
bool FileWatcher::shouldIgnoreChanges() { return false; }

// YubiKey mock moved to keepassxc-jni.cpp

// ModifiableObject signals
void ModifiableObject::emitModifiedChanged(bool) {}
void ModifiableObject::modified() {}

// Database signals
void Database::databaseOpened() {}
void Database::databaseSaved() {}
void Database::databaseDiscarded() {}
void Database::filePathChanged(const QString&, const QString&) {}
void Database::tagListUpdated() {}
void Database::databaseNonDataChanged() {}

// Config signals
void Config::changed(Config::ConfigKey) {}

// QXmlStreamReader mock methods
bool QXmlStreamReader::readNextStartElement() { return false; }
QXmlStreamReader::TokenType QXmlStreamReader::readNext() { return NoToken; }
void QXmlStreamReader::skipCurrentElement() {}

// QTimer mock methods
void QTimer::timeout() {}

// QCoreApplication static mock variables
const char* QCoreApplication::aboutToQuit = "2aboutToQuit()";

#include "core/Group.h"
#include "core/EntryAttributes.h"
#include "core/AutoTypeAssociations.h"
#include <cstdint>

// Group signals
void Group::groupNonDataChange() {}
void Group::groupAboutToRemove(Group*) {}
void Group::groupRemoved() {}
void Group::groupDataChanged(Group*) {}
void Group::groupAboutToAdd(Group*, int) {}
void Group::aboutToMove(Group*, Group*, int) {}
void Group::groupAdded() {}
void Group::groupMoved() {}
void Group::entryAboutToAdd(Entry*) {}
void Group::entryDataChanged(Entry*) {}
void Group::entryAdded(Entry*) {}
void Group::entryAboutToRemove(Entry*) {}
void Group::entryRemoved(Entry*) {}
void Group::entryAboutToMoveUp(int) {}
void Group::entryMovedUp() {}
void Group::entryAboutToMoveDown(int) {}
void Group::entryMovedDown() {}

// Entry signals
void Entry::entryDataChanged(Entry*) {}

// EntryAttributes signals
void EntryAttributes::aboutToBeReset() {}
void EntryAttributes::reset() {}
void EntryAttributes::aboutToBeRemoved(const QString&) {}
void EntryAttributes::removed(const QString&) {}
void EntryAttributes::aboutToBeAdded(const QString&) {}
void EntryAttributes::defaultKeyModified() {}
void EntryAttributes::added(const QString&) {}
void EntryAttributes::customKeyModified(const QString&) {}
void EntryAttributes::aboutToRename(const QString&, const QString&) {}
void EntryAttributes::renamed(const QString&, const QString&) {}

// Additional Database signals
void Database::groupDataChanged(Group*) {}
void Database::groupAboutToRemove(Group*) {}
void Database::groupRemoved() {}
void Database::groupAboutToAdd(Group*, int) {}
void Database::groupAdded() {}
void Database::groupAboutToMove(Group*, Group*, int) {}
void Database::groupMoved() {}

// AutoTypeAssociations signals
void AutoTypeAssociations::aboutToAdd(int) {}
void AutoTypeAssociations::added(int) {}
void AutoTypeAssociations::aboutToRemove(int) {}
void AutoTypeAssociations::removed(int) {}
void AutoTypeAssociations::dataChanged(int) {}
void AutoTypeAssociations::aboutToReset() {}
void AutoTypeAssociations::reset() {}



