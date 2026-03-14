#include <iostream>
#include "android/app/src/main/cpp/pure_cpp_core/include/QtCore/QByteArray"
#include "android/app/src/main/cpp/pure_cpp_core/include/QtCore/QString"

int main() {
    QByteArray ba;
    ba.push_back((char)0x1d);
    ba.push_back((char)0x77);
    ba.push_back((char)0x2f);
    ba.push_back((char)0xd3);
    ba.push_back((char)0x0e);
    ba.push_back((char)0xc4);
    ba.push_back((char)0x4e);
    ba.push_back((char)0xec);
    ba.push_back((char)0xba);
    ba.push_back((char)0xeb);
    ba.push_back((char)0x6d);
    ba.push_back((char)0x8f);
    ba.push_back((char)0x9d);
    ba.push_back((char)0x7d);
    ba.push_back((char)0xdc);
    ba.push_back((char)0x8d);

    QString hex = QString::fromLatin1(ba.toHex());
    std::cout << "Hex: " << hex.toStdString() << std::endl;
    return 0;
}
