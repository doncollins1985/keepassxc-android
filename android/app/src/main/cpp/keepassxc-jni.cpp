#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>

#include "core/Database.h"
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
        jstring title,
        jstring username,
        jstring password,
        jstring url,
        jstring notes) {
    if (dbPtr == 0) return env->NewStringUTF("");

    Database* db = reinterpret_cast<Database*>(dbPtr);
    Group* rootGroup = db->rootGroup();
    if (!rootGroup) return env->NewStringUTF("");

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

    entry->setGroup(rootGroup);

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
