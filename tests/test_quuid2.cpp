#include <iostream>
#include "android/app/src/main/cpp/pure_cpp_core/include/QtCore/QUuid"
#include "android/app/src/main/cpp/pure_cpp_core/include/QtCore/QString"

int main() {
    QUuid uuid = QUuid::createUuid();
    QByteArray rfc = uuid.toRfc4122();
    QByteArray hex = rfc.toHex();
    std::cout << "Hex size: " << hex.size() << std::endl;
    QString s = QString::fromUtf8(hex.constData());
    std::cout << "Hex str: " << s.toStdString() << " length: " << s.length() << std::endl;
    return 0;
}
