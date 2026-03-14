#include <iostream>
#include "android/app/src/main/cpp/pure_cpp_core/include/QtCore/QByteArray"
#include "android/app/src/main/cpp/pure_cpp_core/include/QtCore/QString"

int main() {
    QByteArray ba = QByteArray::fromHex("01020304");
    QString s = QString(ba);
    QByteArray u8 = s.toUtf8();
    std::cout << "Utf8 chars array check: " << (int)u8.constData()[4] << std::endl;
    return 0;
}
