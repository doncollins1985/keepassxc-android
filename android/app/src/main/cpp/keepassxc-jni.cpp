#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>

#include "core/Database.h"
#include "core/Metadata.h"
#include "core/Group.h"
#include "core/Entry.h"
#include "keys/CompositeKey.h"
#include "keys/PasswordKey.h"
#include "keys/ChallengeResponseKey.h"
#include "keys/FileKey.h"
#include "crypto/kdf/AesKdf.h"

#define LOG_TAG "KeePassXCore"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#include "keys/drivers/YubiKey.h"

static JavaVM* g_jvm = nullptr;
static jobject g_yubiKeyCallback = nullptr;

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* /*reserved*/) {
    g_jvm = vm;
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNICALL
Java_org_keepassxc_android_NativeCore_setYubiKeyCallback(JNIEnv* env, jobject /*thiz*/, jobject callback) {
    if (g_yubiKeyCallback) {
        env->DeleteGlobalRef(g_yubiKeyCallback);
        g_yubiKeyCallback = nullptr;
    }
    if (callback) {
        g_yubiKeyCallback = env->NewGlobalRef(callback);
    }
}

YubiKey* YubiKey::instance() {
    static YubiKey* inst = nullptr;
    if (!inst) inst = new YubiKey();
    return inst;
}

YubiKey::YubiKey() {}

QString YubiKey::errorMessage() { return "YubiKey callback failed"; }

YubiKey::ChallengeResult YubiKey::challenge(YubiKeySlot, const QByteArray& challenge, Botan::secure_vector<char>& response) {
    if (!g_jvm || !g_yubiKeyCallback) return YubiKey::ChallengeResult::YCR_ERROR;

    JNIEnv* env = nullptr;
    bool didAttach = false;
    if (g_jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) == JNI_EDETACHED) {
        g_jvm->AttachCurrentThread(&env, nullptr);
        didAttach = true;
    }

    if (!env) return YubiKey::ChallengeResult::YCR_ERROR;

    jclass callbackClass = env->GetObjectClass(g_yubiKeyCallback);
    jmethodID methodId = env->GetMethodID(callbackClass, "onChallenge", "([B)[B");
    env->DeleteLocalRef(callbackClass);

    if (!methodId) {
        if (didAttach) g_jvm->DetachCurrentThread();
        return YubiKey::ChallengeResult::YCR_ERROR;
    }

    jbyteArray jChallenge = env->NewByteArray(challenge.size());
    env->SetByteArrayRegion(jChallenge, 0, challenge.size(), reinterpret_cast<const jbyte*>(challenge.data()));

    jbyteArray jResponse = (jbyteArray)env->CallObjectMethod(g_yubiKeyCallback, methodId, jChallenge);
    env->DeleteLocalRef(jChallenge);

    YubiKey::ChallengeResult result = YubiKey::ChallengeResult::YCR_ERROR;

    if (jResponse) {
        jsize len = env->GetArrayLength(jResponse);
        jbyte* body = env->GetByteArrayElements(jResponse, nullptr);
        
        response.clear();
        for (int i = 0; i < len; ++i) {
            response.push_back(body[i]);
        }
        
        env->ReleaseByteArrayElements(jResponse, body, JNI_ABORT);
        env->DeleteLocalRef(jResponse);
        result = YubiKey::ChallengeResult::YCR_SUCCESS;
    }

    if (didAttach) {
        g_jvm->DetachCurrentThread();
    }

    return result;
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_keepassxc_android_NativeCore_stringFromJNI(
        JNIEnv* env,
        jobject /* thiz */) {
    std::string hello = "Hello from KeePassXC Core (No-Qt)";
    return env->NewStringUTF(hello.c_str());
}

#include "format/KeePass2Reader.h"
#include "format/KeePass2Writer.h"
#include "QtCore/QBuffer"

extern "C" JNIEXPORT jlong JNICALL
Java_org_keepassxc_android_NativeCore_openDatabaseFromBytes(
        JNIEnv* env,
        jobject /* thiz */,
        jbyteArray data,
        jstring password,
        jbyteArray keyfileBytes,
        jboolean useYubiKey) {
    
    jsize len = env->GetArrayLength(data);
    jbyte* body = env->GetByteArrayElements(data, nullptr);
    
    QByteArray qData(reinterpret_cast<const char*>(body), len);
    env->ReleaseByteArrayElements(data, body, JNI_ABORT);

    const char* nativePassword = env->GetStringUTFChars(password, nullptr);
    QString qPassword = QString::fromUtf8(nativePassword);

    auto key = QSharedPointer<CompositeKey>::create();
    if (!qPassword.isEmpty()) {
        key->addKey(QSharedPointer<PasswordKey>::create(qPassword));
    }
    
    if (keyfileBytes != nullptr) {
        jsize klen = env->GetArrayLength(keyfileBytes);
        jbyte* kbody = env->GetByteArrayElements(keyfileBytes, nullptr);
        QByteArray qKeyData(reinterpret_cast<const char*>(kbody), klen);
        env->ReleaseByteArrayElements(keyfileBytes, kbody, JNI_ABORT);
        
        QBuffer kBuffer(&qKeyData);
        kBuffer.open(QIODevice::ReadOnly);
        auto fileKey = QSharedPointer<FileKey>::create();
        QString errorMsg;
        if (fileKey->load(&kBuffer, &errorMsg)) {
            key->addKey(fileKey);
        } else {
            LOGE("Failed to load keyfile: %s", errorMsg.toUtf8().constData());
            env->ReleaseStringUTFChars(password, nativePassword);
            return 0;
        }
    }
    
    if (useYubiKey) {
        key->addChallengeResponseKey(QSharedPointer<ChallengeResponseKey>::create());
    }

    Database* db = new Database();
    QBuffer buffer(&qData);
    buffer.open(QIODevice::ReadOnly);

    KeePass2Reader reader;
    bool success = reader.readDatabase(&buffer, key, db);
    
    env->ReleaseStringUTFChars(password, nativePassword);

    if (success) {
        LOGI("Database opened successfully from bytes");
        return reinterpret_cast<jlong>(db);
    } else {
        LOGE("Failed to open database from bytes: %s", reader.errorString().toUtf8().constData());
        delete db;
        return 0;
    }
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_keepassxc_android_NativeCore_createDatabase(
        JNIEnv* env,
        jobject /* thiz */,
        jstring password,
        jbyteArray keyfileBytes,
        jboolean useYubiKey) {

    auto key = QSharedPointer<CompositeKey>::create();
    
    if (password) {
        const char* nativePassword = env->GetStringUTFChars(password, nullptr);
        QString qPassword = QString::fromUtf8(nativePassword);
        if (!qPassword.isEmpty()) {
            key->addKey(QSharedPointer<PasswordKey>::create(qPassword));
        }
        env->ReleaseStringUTFChars(password, nativePassword);
    }
    
    if (keyfileBytes != nullptr) {
        jsize klen = env->GetArrayLength(keyfileBytes);
        jbyte* kbody = env->GetByteArrayElements(keyfileBytes, nullptr);
        QByteArray qKeyData(reinterpret_cast<const char*>(kbody), klen);
        env->ReleaseByteArrayElements(keyfileBytes, kbody, JNI_ABORT);
        
        QBuffer kBuffer(&qKeyData);
        kBuffer.open(QIODevice::ReadOnly);
        auto fileKey = QSharedPointer<FileKey>::create();
        QString errorMsg;
        if (fileKey->load(&kBuffer, &errorMsg)) {
            key->addKey(fileKey);
        } else {
            LOGE("Failed to load keyfile for new DB: %s", errorMsg.toUtf8().constData());
            return 0;
        }
    }
    
    if (useYubiKey) {
        key->addChallengeResponseKey(QSharedPointer<ChallengeResponseKey>::create());
    }

    Database* db = new Database();
    db->setKey(key);
    db->setKdf(QSharedPointer<AesKdf>::create());
    
    Group* root = new Group();
    root->setName("Root");
    db->setRootGroup(root);

    LOGI("New database created successfully");
    return reinterpret_cast<jlong>(db);
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_org_keepassxc_android_NativeCore_createKeyfileAsBytes(
        JNIEnv* env,
        jobject /* thiz */) {

    QByteArray qData;
    QBuffer buffer(&qData);
    buffer.open(QIODevice::WriteOnly);

    // KeepassXC v2 XML Keyfile defaults to 32 bytes (256-bit)
    FileKey::createXMLv2(&buffer, 32);

    jbyteArray result = env->NewByteArray(qData.size());
    env->SetByteArrayRegion(result, 0, qData.size(), reinterpret_cast<const jbyte*>(qData.data()));
    return result;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_org_keepassxc_android_NativeCore_saveDatabaseAsBytes(
        JNIEnv* env,
        jobject /* thiz */,
        jlong dbPtr) {
    if (dbPtr == 0) return nullptr;

    Database* db = reinterpret_cast<Database*>(dbPtr);

    QByteArray qData;
    QBuffer buffer(&qData);
    buffer.open(QIODevice::WriteOnly);

    KeePass2Writer writer;
    bool success = writer.writeDatabase(&buffer, db);

    if (success) {
        LOGI("Database saved to bytes successfully");
        jbyteArray result = env->NewByteArray(qData.size());
        env->SetByteArrayRegion(result, 0, qData.size(), reinterpret_cast<const jbyte*>(qData.data()));
        return result;
    } else {
        LOGE("Failed to save database to bytes: %s", writer.errorString().toUtf8().constData());
        return nullptr;
    }
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_keepassxc_android_NativeCore_openDatabase(
        JNIEnv* env,
        jobject /* thiz */,
        jstring filePath,
        jstring password,
        jboolean useYubiKey) {
    
    const char* nativeFilePath = env->GetStringUTFChars(filePath, nullptr);
    const char* nativePassword = env->GetStringUTFChars(password, nullptr);

    LOGI("Attempting to open database: %s", nativeFilePath);

    QString qFilePath = QString::fromUtf8(nativeFilePath);
    QString qPassword = QString::fromUtf8(nativePassword);

    auto key = QSharedPointer<CompositeKey>::create();
    if (!qPassword.isEmpty()) {
        key->addKey(QSharedPointer<PasswordKey>::create(qPassword));
    }
    if (useYubiKey) {
        key->addChallengeResponseKey(QSharedPointer<ChallengeResponseKey>::create());
    }

    Database* db = new Database();
    QString error;
    bool success = db->open(qFilePath, key, &error);
    
    env->ReleaseStringUTFChars(filePath, nativeFilePath);
    env->ReleaseStringUTFChars(password, nativePassword);

    if (success) {
        LOGI("Database opened successfully");
        return reinterpret_cast<jlong>(db);
    } else {
        LOGE("Failed to open database: %s", error.toUtf8().constData());
        delete db;
        return 0;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_org_keepassxc_android_NativeCore_closeDatabase(
        JNIEnv* env,
        jobject /* thiz */,
        jlong dbPtr) {
    if (dbPtr != 0) {
        Database* db = reinterpret_cast<Database*>(dbPtr);
        LOGI("Closing database");
        delete db;
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_keepassxc_android_NativeCore_getRootGroupName(
        JNIEnv* env,
        jobject /* thiz */,
        jlong dbPtr) {
    if (dbPtr == 0) return env->NewStringUTF("");

    Database* db = reinterpret_cast<Database*>(dbPtr);
    Group* rootGroup = db->rootGroup();
    if (rootGroup) {
        return env->NewStringUTF(rootGroup->name().toUtf8().constData());
    }

    return env->NewStringUTF("Unknown Root");
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_keepassxc_android_NativeCore_getTotp(
        JNIEnv* env,
        jobject /* thiz */,
        jlong dbPtr,
        jstring entryUuidHex) {
    if (dbPtr == 0) return nullptr;

    const char* nativeUuidHex = env->GetStringUTFChars(entryUuidHex, nullptr);
    QUuid uuid = QUuid::fromHex(nativeUuidHex);
    env->ReleaseStringUTFChars(entryUuidHex, nativeUuidHex);

    Database* db = reinterpret_cast<Database*>(dbPtr);
    Group* rootGroup = db->rootGroup();
    if (!rootGroup) return nullptr;

    Entry* entry = rootGroup->findEntryByUuid(uuid, true);
    if (!entry || !entry->hasTotp()) return nullptr;

    return env->NewStringUTF(entry->totp().toUtf8().constData());
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_keepassxc_android_NativeCore_getEntryDetails(
        JNIEnv* env,
        jobject /* thiz */,
        jlong dbPtr,
        jstring uuidHex) {
    
    if (dbPtr == 0) return nullptr;

    const char* nativeUuidHex = env->GetStringUTFChars(uuidHex, nullptr);
    QUuid uuid = QUuid::fromHex(nativeUuidHex);
    env->ReleaseStringUTFChars(uuidHex, nativeUuidHex);

    Database* db = reinterpret_cast<Database*>(dbPtr);
    Group* rootGroup = db->rootGroup();
    if (!rootGroup) return nullptr;

    Entry* entry = rootGroup->findEntryByUuid(uuid, true);
    if (!entry) return nullptr;

    jclass detailClass = env->FindClass("org/keepassxc/android/models/EntryDetails");
    // Constructor: EntryDetails(String title, String username, String password, String url, String notes, String uuid, String totp)
    jmethodID detailConstructor = env->GetMethodID(detailClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");

    jstring title = env->NewStringUTF(entry->title().toUtf8().constData());
    jstring username = env->NewStringUTF(entry->username().toUtf8().constData());
    jstring password = env->NewStringUTF(entry->password().toUtf8().constData());
    jstring url = env->NewStringUTF(entry->url().toUtf8().constData());
    jstring notes = env->NewStringUTF(entry->notes().toUtf8().constData());
    jstring uuidStr = env->NewStringUTF(entry->uuidToHex().toUtf8().constData());
    
    jstring totp = nullptr;
    if (entry->hasTotp()) {
        totp = env->NewStringUTF(entry->totp().toUtf8().constData());
    }

    jobject detailObj = env->NewObject(detailClass, detailConstructor, title, username, password, url, notes, uuidStr, totp);

    env->DeleteLocalRef(title);
    env->DeleteLocalRef(username);
    env->DeleteLocalRef(password);
    env->DeleteLocalRef(url);
    env->DeleteLocalRef(notes);
    env->DeleteLocalRef(uuidStr);
    if (totp) env->DeleteLocalRef(totp);

    return detailObj;
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_keepassxc_android_NativeCore_addEntry(
        JNIEnv* env,
        jobject /* thiz */,
        jlong dbPtr,
        jstring groupUuidHex,
        jstring title,
        jstring username,
        jstring password,
        jstring url,
        jstring notes) {
    if (dbPtr == 0) return env->NewStringUTF("");

    const char* nativeGroupUuidHex = env->GetStringUTFChars(groupUuidHex, nullptr);
    QUuid groupUuid = QUuid::fromHex(nativeGroupUuidHex);
    env->ReleaseStringUTFChars(groupUuidHex, nativeGroupUuidHex);

    Database* db = reinterpret_cast<Database*>(dbPtr);
    Group* rootGroup = db->rootGroup();
    if (!rootGroup) return env->NewStringUTF("");

    Group* group = rootGroup->findGroupByUuid(groupUuid);
    if (!group) group = rootGroup;

    Entry* entry = new Entry();
    entry->setUuid(QUuid::createUuid());
    
    if (title) {
        const char* t = env->GetStringUTFChars(title, nullptr);
        entry->setTitle(QString::fromUtf8(t));
        env->ReleaseStringUTFChars(title, t);
    }
    if (username) {
        const char* u = env->GetStringUTFChars(username, nullptr);
        entry->setUsername(QString::fromUtf8(u));
        env->ReleaseStringUTFChars(username, u);
    }
    if (password) {
        const char* p = env->GetStringUTFChars(password, nullptr);
        entry->setPassword(QString::fromUtf8(p));
        env->ReleaseStringUTFChars(password, p);
    }
    if (url) {
        const char* u = env->GetStringUTFChars(url, nullptr);
        entry->setUrl(QString::fromUtf8(u));
        env->ReleaseStringUTFChars(url, u);
    }
    if (notes) {
        const char* n = env->GetStringUTFChars(notes, nullptr);
        entry->setNotes(QString::fromUtf8(n));
        env->ReleaseStringUTFChars(notes, n);
    }

    entry->setGroup(group);

    return env->NewStringUTF(entry->uuidToHex().toUtf8().constData());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_keepassxc_android_NativeCore_updateEntry(
        JNIEnv* env,
        jobject /* thiz */,
        jlong dbPtr,
        jstring uuidHex,
        jstring title,
        jstring username,
        jstring password,
        jstring url,
        jstring notes) {
    if (dbPtr == 0) return false;

    const char* nativeUuidHex = env->GetStringUTFChars(uuidHex, nullptr);
    QUuid uuid = QUuid::fromHex(nativeUuidHex);
    env->ReleaseStringUTFChars(uuidHex, nativeUuidHex);

    Database* db = reinterpret_cast<Database*>(dbPtr);
    Group* rootGroup = db->rootGroup();
    if (!rootGroup) return false;

    Entry* entry = rootGroup->findEntryByUuid(uuid, true);
    if (!entry) return false;

    if (title) {
        const char* t = env->GetStringUTFChars(title, nullptr);
        entry->setTitle(QString::fromUtf8(t));
        env->ReleaseStringUTFChars(title, t);
    }
    if (username) {
        const char* u = env->GetStringUTFChars(username, nullptr);
        entry->setUsername(QString::fromUtf8(u));
        env->ReleaseStringUTFChars(username, u);
    }
    if (password) {
        const char* p = env->GetStringUTFChars(password, nullptr);
        entry->setPassword(QString::fromUtf8(p));
        env->ReleaseStringUTFChars(password, p);
    }
    if (url) {
        const char* u = env->GetStringUTFChars(url, nullptr);
        entry->setUrl(QString::fromUtf8(u));
        env->ReleaseStringUTFChars(url, u);
    }
    if (notes) {
        const char* n = env->GetStringUTFChars(notes, nullptr);
        entry->setNotes(QString::fromUtf8(n));
        env->ReleaseStringUTFChars(notes, n);
    }

    return true;
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_keepassxc_android_NativeCore_getEntries(
        JNIEnv* env,
        jobject /* thiz */,
        jlong dbPtr) {
    
    jclass listClass = env->FindClass("java/util/ArrayList");
    jmethodID listConstructor = env->GetMethodID(listClass, "<init>", "()V");
    jmethodID listAdd = env->GetMethodID(listClass, "add", "(Ljava/lang/Object;)Z");
    jobject list = env->NewObject(listClass, listConstructor);

    if (dbPtr == 0) return list;

    Database* db = reinterpret_cast<Database*>(dbPtr);
    Group* rootGroup = db->rootGroup();
    if (!rootGroup) return list;

    jclass entryClass = env->FindClass("org/keepassxc/android/models/Entry");
    jmethodID entryConstructor = env->GetMethodID(entryClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");

    QList<Entry*> entries = rootGroup->entriesRecursive();
    for (Entry* entry : entries) {
        jstring title = env->NewStringUTF(entry->title().toUtf8().constData());
        jstring username = env->NewStringUTF(entry->username().toUtf8().constData());
        jstring uuid = env->NewStringUTF(entry->uuidToHex().toUtf8().constData());

        jobject entryObj = env->NewObject(entryClass, entryConstructor, title, username, uuid);
        env->CallBooleanMethod(list, listAdd, entryObj);

        env->DeleteLocalRef(title);
        env->DeleteLocalRef(username);
        env->DeleteLocalRef(uuid);
        env->DeleteLocalRef(entryObj);
    }

    return list;
}

static void recursiveGetGroups(JNIEnv* env, jobject list, jmethodID listAdd, jclass groupNodeClass, jmethodID groupNodeConstructor, Group* group, int depth, const QString& path) {
    QString currentPath = path.isEmpty() ? group->name() : path + "/" + group->name();

    jstring name = env->NewStringUTF(group->name().toUtf8().constData());
    jstring uuid = env->NewStringUTF(group->uuidToHex().toUtf8().constData());
    jstring fullPath = env->NewStringUTF(currentPath.toUtf8().constData());
    
    jobject node = env->NewObject(groupNodeClass, groupNodeConstructor, name, uuid, (jint)depth, fullPath);
    env->CallBooleanMethod(list, listAdd, node);

    env->DeleteLocalRef(name);
    env->DeleteLocalRef(uuid);
    env->DeleteLocalRef(fullPath);
    env->DeleteLocalRef(node);

    for (Group* child : group->children()) {
        recursiveGetGroups(env, list, listAdd, groupNodeClass, groupNodeConstructor, child, depth + 1, currentPath);
    }
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_keepassxc_android_NativeCore_getGroups(
        JNIEnv* env,
        jobject /* thiz */,
        jlong dbPtr) {
    
    jclass listClass = env->FindClass("java/util/ArrayList");
    jmethodID listConstructor = env->GetMethodID(listClass, "<init>", "()V");
    jmethodID listAdd = env->GetMethodID(listClass, "add", "(Ljava/lang/Object;)Z");
    jobject list = env->NewObject(listClass, listConstructor);

    if (dbPtr == 0) return list;

    Database* db = reinterpret_cast<Database*>(dbPtr);
    Group* rootGroup = db->rootGroup();
    if (!rootGroup) return list;

    jclass groupNodeClass = env->FindClass("org/keepassxc/android/models/GroupNode");
    jmethodID groupNodeConstructor = env->GetMethodID(groupNodeClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)V");

    recursiveGetGroups(env, list, listAdd, groupNodeClass, groupNodeConstructor, rootGroup, 0, "");

    return list;
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_keepassxc_android_NativeCore_getEntriesInGroup(
        JNIEnv* env,
        jobject /* thiz */,
        jlong dbPtr,
        jstring groupUuidHex) {
    
    jclass listClass = env->FindClass("java/util/ArrayList");
    jmethodID listConstructor = env->GetMethodID(listClass, "<init>", "()V");
    jmethodID listAdd = env->GetMethodID(listClass, "add", "(Ljava/lang/Object;)Z");
    jobject list = env->NewObject(listClass, listConstructor);

    if (dbPtr == 0) return list;

    const char* nativeUuidHex = env->GetStringUTFChars(groupUuidHex, nullptr);
    QUuid uuid = QUuid::fromHex(nativeUuidHex);
    env->ReleaseStringUTFChars(groupUuidHex, nativeUuidHex);

    Database* db = reinterpret_cast<Database*>(dbPtr);
    Group* rootGroup = db->rootGroup();
    if (!rootGroup) return list;

    Group* group = (uuid.isNull()) ? rootGroup : rootGroup->findGroupByUuid(uuid);
    if (!group) return list;

    jclass entryClass = env->FindClass("org/keepassxc/android/models/Entry");
    jmethodID entryConstructor = env->GetMethodID(entryClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");

    for (Entry* entry : group->entries()) {
        jstring title = env->NewStringUTF(entry->title().toUtf8().constData());
        jstring username = env->NewStringUTF(entry->username().toUtf8().constData());
        jstring uuid = env->NewStringUTF(entry->uuidToHex().toUtf8().constData());

        jobject entryObj = env->NewObject(entryClass, entryConstructor, title, username, uuid);
        env->CallBooleanMethod(list, listAdd, entryObj);

        env->DeleteLocalRef(title);
        env->DeleteLocalRef(username);
        env->DeleteLocalRef(uuid);
        env->DeleteLocalRef(entryObj);
    }

    return list;
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_keepassxc_android_NativeCore_addGroup(
        JNIEnv* env,
        jobject /* thiz */,
        jlong dbPtr,
        jstring parentUuidHex,
        jstring name) {
    if (dbPtr == 0) return env->NewStringUTF("");

    const char* nativeParentUuidHex = env->GetStringUTFChars(parentUuidHex, nullptr);
    QUuid parentUuid = QUuid::fromHex(nativeParentUuidHex);
    env->ReleaseStringUTFChars(parentUuidHex, nativeParentUuidHex);

    Database* db = reinterpret_cast<Database*>(dbPtr);
    Group* rootGroup = db->rootGroup();
    if (!rootGroup) return env->NewStringUTF("");

    Group* parent = rootGroup->findGroupByUuid(parentUuid);
    if (!parent) parent = rootGroup;

    Group* group = new Group();
    group->setUuid(QUuid::createUuid());
    
    if (name) {
        const char* n = env->GetStringUTFChars(name, nullptr);
        group->setName(QString::fromUtf8(n));
        env->ReleaseStringUTFChars(name, n);
    }

    group->setParent(parent);

    return env->NewStringUTF(group->uuidToHex().toUtf8().constData());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_keepassxc_android_NativeCore_renameGroup(
        JNIEnv* env,
        jobject /* thiz */,
        jlong dbPtr,
        jstring groupUuidHex,
        jstring newName) {
    if (dbPtr == 0) return false;

    const char* nativeUuidHex = env->GetStringUTFChars(groupUuidHex, nullptr);
    QUuid uuid = QUuid::fromHex(nativeUuidHex);
    env->ReleaseStringUTFChars(groupUuidHex, nativeUuidHex);

    Database* db = reinterpret_cast<Database*>(dbPtr);
    Group* rootGroup = db->rootGroup();
    if (!rootGroup) return false;

    Group* group = rootGroup->findGroupByUuid(uuid);
    if (!group) return false;

    if (newName) {
        const char* n = env->GetStringUTFChars(newName, nullptr);
        group->setName(QString::fromUtf8(n));
        env->ReleaseStringUTFChars(newName, n);
    }

    return true;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_keepassxc_android_NativeCore_deleteGroup(
        JNIEnv* env,
        jobject /* thiz */,
        jlong dbPtr,
        jstring groupUuidHex) {
    if (dbPtr == 0) return false;

    const char* nativeUuidHex = env->GetStringUTFChars(groupUuidHex, nullptr);
    QUuid uuid = QUuid::fromHex(nativeUuidHex);
    env->ReleaseStringUTFChars(groupUuidHex, nativeUuidHex);

    Database* db = reinterpret_cast<Database*>(dbPtr);
    Group* rootGroup = db->rootGroup();
    if (!rootGroup) return false;

    Group* group = rootGroup->findGroupByUuid(uuid);
    if (!group || group == rootGroup) return false;

    Group* recycleBin = db->metadata()->recycleBin();
    if (recycleBin && group != recycleBin && !recycleBin->groupsRecursive(true).contains(group)) {
        db->recycleGroup(group);
    } else {
        delete group;
    }

    return true;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_keepassxc_android_NativeCore_moveEntry(
        JNIEnv* env,
        jobject /* thiz */,
        jlong dbPtr,
        jstring entryUuidHex,
        jstring groupUuidHex) {
    if (dbPtr == 0) return false;

    const char* nativeEntryUuidHex = env->GetStringUTFChars(entryUuidHex, nullptr);
    QUuid entryUuid = QUuid::fromHex(nativeEntryUuidHex);
    env->ReleaseStringUTFChars(entryUuidHex, nativeEntryUuidHex);

    const char* nativeGroupUuidHex = env->GetStringUTFChars(groupUuidHex, nullptr);
    QUuid groupUuid = QUuid::fromHex(nativeGroupUuidHex);
    env->ReleaseStringUTFChars(groupUuidHex, nativeGroupUuidHex);

    Database* db = reinterpret_cast<Database*>(dbPtr);
    Group* rootGroup = db->rootGroup();
    if (!rootGroup) return false;

    Entry* entry = rootGroup->findEntryByUuid(entryUuid, true);
    if (!entry) return false;

    Group* group = rootGroup->findGroupByUuid(groupUuid);
    if (!group) return false;

    entry->setGroup(group);
    return true;
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_keepassxc_android_NativeCore_getEntryHistory(
        JNIEnv* env,
        jobject /* thiz */,
        jlong dbPtr,
        jstring entryUuidHex) {
    
    jclass listClass = env->FindClass("java/util/ArrayList");
    jmethodID listConstructor = env->GetMethodID(listClass, "<init>", "()V");
    jmethodID listAdd = env->GetMethodID(listClass, "add", "(Ljava/lang/Object;)Z");
    jobject list = env->NewObject(listClass, listConstructor);

    if (dbPtr == 0) return list;

    const char* nativeUuidHex = env->GetStringUTFChars(entryUuidHex, nullptr);
    QUuid uuid = QUuid::fromHex(nativeUuidHex);
    env->ReleaseStringUTFChars(entryUuidHex, nativeUuidHex);

    Database* db = reinterpret_cast<Database*>(dbPtr);
    Group* rootGroup = db->rootGroup();
    if (!rootGroup) return list;

    Entry* entry = rootGroup->findEntryByUuid(uuid, true);
    if (!entry) return list;

    jclass historyItemClass = env->FindClass("org/keepassxc/android/models/EntryHistoryItem");
    jmethodID historyItemConstructor = env->GetMethodID(historyItemClass, "<init>", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;J)V");

    QList<Entry*> history = entry->historyItems();
    for (int i = 0; i < history.size(); ++i) {
        Entry* h = history[i];
        jstring title = env->NewStringUTF(h->title().toUtf8().constData());
        jstring username = env->NewStringUTF(h->username().toUtf8().constData());
        jstring url = env->NewStringUTF(h->url().toUtf8().constData());
        jstring uuidStr = env->NewStringUTF(h->uuidToHex().toUtf8().constData());
        jlong modified = h->timeInfo().lastModificationTime().toMSecsSinceEpoch();

        jobject item = env->NewObject(historyItemClass, historyItemConstructor, i, title, username, url, uuidStr, modified);
        env->CallBooleanMethod(list, listAdd, item);

        env->DeleteLocalRef(title);
        env->DeleteLocalRef(username);
        env->DeleteLocalRef(url);
        env->DeleteLocalRef(uuidStr);
        env->DeleteLocalRef(item);
    }

    return list;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_keepassxc_android_NativeCore_restoreEntryHistory(
        JNIEnv* env,
        jobject /* thiz */,
        jlong dbPtr,
        jstring entryUuidHex,
        jstring historyUuidHex) {
    if (dbPtr == 0) return false;

    const char* nativeUuidHex = env->GetStringUTFChars(entryUuidHex, nullptr);
    QUuid uuid = QUuid::fromHex(nativeUuidHex);
    env->ReleaseStringUTFChars(entryUuidHex, nativeUuidHex);

    const char* nativeHistoryUuidHex = env->GetStringUTFChars(historyUuidHex, nullptr);
    QUuid historyUuid = QUuid::fromHex(nativeHistoryUuidHex);
    env->ReleaseStringUTFChars(historyUuidHex, nativeHistoryUuidHex);

    Database* db = reinterpret_cast<Database*>(dbPtr);
    Group* rootGroup = db->rootGroup();
    if (!rootGroup) return false;

    Entry* entry = rootGroup->findEntryByUuid(uuid, true);
    if (!entry) return false;

    for (Entry* h : entry->historyItems()) {
        if (h->uuid() == historyUuid) {
            entry->copyDataFrom(h);
            return true;
        }
    }

    return false;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_keepassxc_android_NativeCore_emptyRecycleBin(
        JNIEnv* env,
        jobject /* thiz */,
        jlong dbPtr) {
    if (dbPtr == 0) return false;

    Database* db = reinterpret_cast<Database*>(dbPtr);
    Group* recycleBin = db->metadata()->recycleBin();
    if (!recycleBin) return false;

    db->emptyRecycleBin();
    return true;
}
