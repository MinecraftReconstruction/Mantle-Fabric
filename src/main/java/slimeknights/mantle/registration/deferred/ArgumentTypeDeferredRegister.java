package slimeknights.mantle.registration.deferred;

import com.mojang.brigadier.arguments.ArgumentType;
import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import slimeknights.mantle.registration.RegistrationHelper;

import java.util.function.Supplier;

/** Register for argument types that automatically handles registering with {@link ArgumentTypeInfos}'s class map */
@SuppressWarnings("UnusedReturnValue")
public class ArgumentTypeDeferredRegister extends DeferredRegisterWrapper<ArgumentTypeInfo<?,?>> {
  public ArgumentTypeDeferredRegister(String modID) {
    super(Registries.COMMAND_ARGUMENT_TYPE, modID);
  }

  /**
   * Registers an argument type
   * @param name           Name of the argument
   * @param argumentClass  Class of the argument
   * @param supplier       Supplier to the argument info
   * @param <A>  Argument type
   * @param <T>  Argument info template type
   * @param <I>  Argument info type
   * @return  Registry object
   */
  public <A extends ArgumentType<?>,T extends ArgumentTypeInfo.Template<A>,I extends ArgumentTypeInfo<A,T>> RegistryObject<I> register(String name, Class<? super A> argumentClass, Supplier<I> supplier) {
    return register.register(name, () -> {
      I info = supplier.get();
      // NOTE(porting): Forge patches in ArgumentTypeInfos#registerByClass to expose this map; vanilla 1.20.1 has no such
      //  method, so we widen the private BY_CLASS map with an access widener and write to it directly. Semantically
      //  identical - it is exactly what ArgumentTypeInfos#register does besides the registry call we already perform.
      //  See docs/BEHAVIOUR-DIFFERENCES.md.
      ArgumentTypeInfos.BY_CLASS.put(RegistrationHelper.genericArgumentType(argumentClass), info);
      return info;
    });
  }

  /**
   * Registers a context free singleton argument
   * @param name           Name of the argument
   * @param argumentClass  Class of the argument
   * @param supplier       Supplier to the argument default
   * @param <A>  Argument type
   * @return  Registry object
   */
  public <A extends ArgumentType<?>> RegistryObject<SingletonArgumentInfo<A>> registerSingleton(String name, Class<A> argumentClass, Supplier<A> supplier) {
    return register(name, argumentClass, () -> SingletonArgumentInfo.contextFree(supplier));
  }
}
